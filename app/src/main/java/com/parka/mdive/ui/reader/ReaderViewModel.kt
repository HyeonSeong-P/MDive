package com.parka.mdive.ui.reader

import com.parka.mdive.domain.document.DocumentLinkRepository
import com.parka.mdive.domain.document.DocumentLinkResult
import com.parka.mdive.domain.document.LinkFailure
import androidx.lifecycle.viewModelScope
import com.parka.mdive.domain.document.ImageRaster
import com.parka.mdive.domain.document.ImageRepository
import com.parka.mdive.domain.document.ImageReadResult
import com.parka.mdive.domain.document.ImageFailure
import com.parka.mdive.domain.document.MarkdownImage
import com.parka.mdive.domain.document.MarkdownMermaid
import com.parka.mdive.domain.document.MarkdownDocument
import com.parka.mdive.domain.document.MarkdownParser
import com.parka.mdive.domain.document.DocumentFailure
import com.parka.mdive.domain.document.DocumentReadResult
import com.parka.mdive.domain.document.DocumentReference
import com.parka.mdive.domain.document.DocumentRepository
import com.parka.mdive.domain.document.SourceDocument
import com.parka.mdive.ui.base.BaseViewModel
import com.parka.mdive.ui.base.UiEffect
import com.parka.mdive.ui.base.UiIntent
import com.parka.mdive.ui.base.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

enum class ReaderContent { Loading, Empty, Failure, SourceReady }

data class ReaderState(
    val title: String? = null,
    val content: ReaderContent = ReaderContent.Loading,
    val document: SourceDocument? = null,
    val failure: DocumentFailure? = null,
    val markdown: MarkdownDocument? = null,
    val mathSource: String? = null,
    val mermaidSource: String? = null,
    val expandedMermaid: MarkdownMermaid? = null,
    val images: Map<String, ImageReadResult> = emptyMap(),
    val imageGeneration: Long = 0,
    val expandedImage: MarkdownImage? = null,
    val expandedImageRaster: ImageRaster? = null,
    val folderPickerPending: Boolean = false,
    val linkFailure: LinkFailure? = null,
    val linkDestination: String? = null,
    val linkLoading: Boolean = false,
    val linkFolderPending: Boolean = false,
) : UiState

sealed interface ReaderIntent : UiIntent {
    data class Open(val reference: DocumentReference, val prepared: PreparedDocument? = null) : ReaderIntent
    data class OpenLink(val destination: String) : ReaderIntent
    data object ConnectDocumentFolder : ReaderIntent
    data class DocumentFolderSelected(val folder: String?) : ReaderIntent
    data object BrowserFailed : ReaderIntent
    data object LinkReturned : ReaderIntent
    data object DismissLinkFailure : ReaderIntent
    data class CopyCode(val source: String) : ReaderIntent
    data object Back : ReaderIntent
    data class ViewMathSource(val source: String) : ReaderIntent
    data object CloseMathSource : ReaderIntent
    data class ViewMermaidSource(val source: String) : ReaderIntent
    data object CloseMermaidSource : ReaderIntent
    data class ExpandMermaid(val diagram: MarkdownMermaid) : ReaderIntent
    data object CloseMermaid : ReaderIntent
    data class ImageVisibility(val image: MarkdownImage, val visible: Boolean) : ReaderIntent
    data class LoadImage(val image: MarkdownImage, val retry: Boolean = false) : ReaderIntent
    data class ExpandImage(val image: MarkdownImage, val raster: ImageRaster? = null) : ReaderIntent
    data object CloseImage : ReaderIntent
    data object ConnectImageFolder : ReaderIntent
    data class ImageFolderSelected(val folder: String?) : ReaderIntent
    data object Retry : ReaderIntent
}

data class PreparedDocument(val source: SourceDocument, val markdown: MarkdownDocument)

sealed interface ReaderEffect : UiEffect {
    data class CopyCode(val source: String) : ReaderEffect
    data object NavigateHome : ReaderEffect
    data object SelectImageFolder : ReaderEffect
    data object SelectDocumentFolder : ReaderEffect
    data class OpenBrowser(val destination: String) : ReaderEffect
    data class NavigateDocument(val reference: DocumentReference, val prepared: PreparedDocument) : ReaderEffect
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: DocumentRepository,
    private val parser: MarkdownParser,
    private val imageRepository: ImageRepository? = null,
    private val links: DocumentLinkRepository? = null,
) : BaseViewModel<ReaderState, ReaderIntent, ReaderEffect>(ReaderState()) {
    private var reference: DocumentReference? = null
    private var readJob: Job? = null
    private var leaving = false
    private val imageJobs = mutableMapOf<String, Job>()
    private val visibleImages = mutableMapOf<String, Int>()
    private var connectJob: Job? = null
    private var imageVersion = ""
    private var linkJob: Job? = null
    private var navigationPending = false


    override fun onIntent(intent: ReaderIntent) {
        when (intent) {
            is ReaderIntent.OpenLink -> if (!leaving && !uiState.value.folderPickerPending && !navigationPending && !uiState.value.linkLoading && !uiState.value.linkFolderPending && uiState.value.content == ReaderContent.SourceReady) openLink(intent.destination)
            ReaderIntent.LinkReturned -> { navigationPending = false }
            ReaderIntent.BrowserFailed -> { navigationPending = false; reduce { copy(linkFailure = LinkFailure.BrowserUnavailable) } }
            ReaderIntent.DismissLinkFailure -> reduce { copy(linkFailure = null, linkDestination = null) }
            ReaderIntent.ConnectDocumentFolder -> if (!leaving && !uiState.value.linkFolderPending && !uiState.value.folderPickerPending && !uiState.value.linkLoading && uiState.value.linkDestination != null) {
                reduce { copy(linkFolderPending = true) }
                viewModelScope.launch { emitEffect(ReaderEffect.SelectDocumentFolder) }
            }
            is ReaderIntent.DocumentFolderSelected -> {
                if (!uiState.value.linkFolderPending) return
                reduce { copy(linkFolderPending = false) }
                if (intent.folder != null && !leaving) uiState.value.linkDestination?.let { openLink(it, intent.folder) }
            }

            is ReaderIntent.ImageVisibility -> {
                val key = intent.image.destination
                if (intent.visible) {
                    visibleImages[key] = (visibleImages[key] ?: 0) + 1
                    loadImage(intent.image, false)
                } else {
                    val count = (visibleImages[key] ?: 1) - 1
                    if (count <= 0) { visibleImages.remove(key); reduce { copy(images = images - key) } }
                    else visibleImages[key] = count
                }
            }
            is ReaderIntent.LoadImage -> loadImage(intent.image, intent.retry)
            is ReaderIntent.ExpandImage -> if (!leaving && uiState.value.content == ReaderContent.SourceReady) {
                val raster = intent.raster ?: (uiState.value.images[intent.image.destination] as? ImageReadResult.Ready)?.raster
                if (raster != null) reduce { copy(expandedImage = intent.image, expandedImageRaster = raster, expandedMermaid = null, mathSource = null, mermaidSource = null) }
            }
            ReaderIntent.CloseImage -> reduce { copy(expandedImage = null, expandedImageRaster = null) }
            ReaderIntent.ConnectImageFolder -> if (!leaving && uiState.value.content == ReaderContent.SourceReady && !uiState.value.folderPickerPending && !uiState.value.linkFolderPending && !uiState.value.linkLoading) {
                reduce { copy(folderPickerPending = true) }
                viewModelScope.launch { emitEffect(ReaderEffect.SelectImageFolder) }
            }
            is ReaderIntent.ImageFolderSelected -> {
                val selected = reference
                if (intent.folder == null) reduce { copy(folderPickerPending = false) }
                if (!leaving && selected != null && intent.folder != null && connectJob?.isActive != true) connectJob = viewModelScope.launch {
                    val failure = imageRepository?.connect(selected, intent.folder) ?: if (imageRepository == null) ImageFailure.Access else null
                    if (leaving) return@launch
                    reduce { copy(folderPickerPending = false) }
                    if (failure != null) {
                        reduce { copy(images = images + ("__folder_failure" to ImageReadResult.Failure(failure))) }
                        return@launch
                    }
                    imageJobs.filterKeys { !it.startsWith("https:", true) }.values.toList().forEach { it.cancel() }
                    imageJobs.keys.removeAll { !it.startsWith("https:", true) }
                    reduce { copy(imageGeneration = imageGeneration + 1, images = images.filterKeys { it.startsWith("https:", true) }.toMutableMap().apply {
                        remove("__folder_failure")
                    }) }
                }
            }
            is ReaderIntent.Open -> if (reference == null && !leaving) {
                reference = intent.reference
                if (intent.prepared != null) showPrepared(intent.prepared) else read()
            }
            ReaderIntent.Retry -> if (!leaving && uiState.value.content == ReaderContent.Failure) read()
            is ReaderIntent.CopyCode -> if (!leaving && uiState.value.content == ReaderContent.SourceReady) {
                viewModelScope.launch { emitEffect(ReaderEffect.CopyCode(intent.source)) }
            }
            is ReaderIntent.ViewMathSource -> if (!leaving && uiState.value.content == ReaderContent.SourceReady) reduce { copy(mathSource = intent.source, mermaidSource = null, expandedMermaid = null, expandedImage = null, expandedImageRaster = null) }
            ReaderIntent.CloseMathSource -> reduce { copy(mathSource = null) }
            is ReaderIntent.ViewMermaidSource -> if (!leaving && uiState.value.content == ReaderContent.SourceReady) reduce { copy(mermaidSource = intent.source, mathSource = null, expandedMermaid = null, expandedImage = null, expandedImageRaster = null) }
            ReaderIntent.CloseMermaidSource -> reduce { copy(mermaidSource = null) }
            is ReaderIntent.ExpandMermaid -> if (!leaving && uiState.value.content == ReaderContent.SourceReady && intent.diagram.supported) reduce { copy(expandedMermaid = intent.diagram, mermaidSource = null, mathSource = null, expandedImage = null, expandedImageRaster = null) }
            ReaderIntent.CloseMermaid -> reduce { copy(expandedMermaid = null, expandedImage = null, expandedImageRaster = null) }
            ReaderIntent.Back -> if (uiState.value.linkLoading || uiState.value.linkFolderPending) {
                linkJob?.cancel()
                reduce { copy(linkLoading = false, linkFolderPending = false) }
            } else if (uiState.value.expandedImage != null) reduce { copy(expandedImage = null, expandedImageRaster = null) }
            else if (uiState.value.expandedMermaid != null) reduce { copy(expandedMermaid = null, expandedImage = null, expandedImageRaster = null) }
            else if (uiState.value.mermaidSource != null) reduce { copy(mermaidSource = null) }
            else if (uiState.value.mathSource != null) reduce { copy(mathSource = null) } else {
                leaving = true
                readJob?.cancel()
                linkJob?.cancel()
                imageJobs.values.toList().forEach { it.cancel() }
                connectJob?.cancel()
                viewModelScope.launch { emitEffect(ReaderEffect.NavigateHome) }
            }
        }
    }


    private fun loadImage(image: MarkdownImage, retry: Boolean) {
        val selected = reference ?: return
        if (leaving || uiState.value.content != ReaderContent.SourceReady) return
        val key = image.destination
        if (!retry && (key in uiState.value.images || imageJobs[key]?.isActive == true)) return
        imageJobs.remove(key)?.cancel()
        val generation = uiState.value.imageGeneration
        reduce { copy(images = images - key) }
        val job = viewModelScope.launch {
            val result = imageRepository?.read(selected, key, retry, imageVersion) ?: ImageReadResult.Failure(ImageFailure.Unsupported)
            ensureActive()
            if (leaving || (!key.startsWith("https:", true) && generation != uiState.value.imageGeneration)) return@launch
            // Only active presentation results live in screen state. Coil owns the bounded
            // memory/disk reuse caches and can deliver a cached image without fetching/decoding.
            if ((visibleImages[key] ?: 0) > 0) reduce { copy(images = images + (key to result)) }
        }
        imageJobs[key] = job
        job.invokeOnCompletion { if (imageJobs[key] === job) imageJobs.remove(key) }
    }


    private fun showPrepared(prepared: PreparedDocument) {
        imageVersion = java.security.MessageDigest.getInstance("SHA-256").digest(prepared.source.source.toByteArray()).joinToString("") { "%02x".format(it) }
        reduce { ReaderState(title = prepared.source.title, content = if (prepared.source.source.isEmpty()) ReaderContent.Empty else ReaderContent.SourceReady, document = prepared.source, markdown = prepared.markdown) }
    }

    private fun openLink(destination: String, folder: String? = null) {
        val selected = reference ?: return
        if (linkJob?.isActive == true) return
        reduce { copy(linkLoading = true, linkDestination = destination, linkFailure = null) }
        linkJob = viewModelScope.launch {
            val result = if (folder == null) links?.resolve(selected, destination) else links?.connect(selected, destination, folder)
            ensureActive()
            if (leaving) return@launch
            when (result) {
                is DocumentLinkResult.Web -> {
                    navigationPending = true
                    reduce { copy(linkLoading = false) }
                    emitEffect(ReaderEffect.OpenBrowser(result.destination))
                }
                is DocumentLinkResult.Ready -> {
                    val read = repository.read(result.reference)
                    ensureActive()
                    if (read is DocumentReadResult.Success) {
                        val parsed = try { parser.parse(read.document.source) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { null }
                        ensureActive()
                        if (parsed != null && !leaving) {
                            navigationPending = true
                            reduce { copy(linkLoading = false, imageGeneration = imageGeneration + if (folder != null) 1 else 0, images = if (folder != null) emptyMap() else images) }
                            emitEffect(ReaderEffect.NavigateDocument(result.reference, PreparedDocument(read.document, parsed)))
                        } else reduce { copy(linkLoading = false, linkFailure = LinkFailure.Read) }
                    } else reduce { copy(linkLoading = false, linkFailure = if ((read as? DocumentReadResult.Failure)?.reason == DocumentFailure.Access) LinkFailure.Access else LinkFailure.Read) }
                }
                is DocumentLinkResult.Failure -> reduce { copy(linkLoading = false, linkFailure = result.reason) }
                null -> reduce { copy(linkLoading = false, linkFailure = LinkFailure.Unsupported) }
            }
        }
    }

    private fun read() {
        val selected = reference ?: return
        if (readJob?.isActive == true) return
        reduce { copy(content = ReaderContent.Loading, document = null, failure = null, markdown = null) }
        readJob = viewModelScope.launch {
            val result = repository.read(selected)
            ensureActive()
            if (leaving) return@launch
            when (result) {
                is DocumentReadResult.Success -> {
                    val markdown = try {
                        parser.parse(result.document.source)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        reduce { copy(title = result.document.title, content = ReaderContent.Failure, failure = DocumentFailure.Read) }
                        return@launch
                    }
                    ensureActive()
                    if (leaving) return@launch
                    imageVersion = java.security.MessageDigest.getInstance("SHA-256").digest(result.document.source.toByteArray()).joinToString("") { "%02x".format(it) }
                    reduce { ReaderState(result.document.title,
                        if (result.document.source.isEmpty()) ReaderContent.Empty else ReaderContent.SourceReady,
                        result.document, markdown = markdown) }
                }
                is DocumentReadResult.Failure -> reduce { copy(content = ReaderContent.Failure, failure = result.reason) }
            }
        }
    }
}

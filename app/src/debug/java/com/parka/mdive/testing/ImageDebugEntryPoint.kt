package com.parka.mdive.testing

import com.parka.mdive.domain.document.ImageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only access to the real image repository for deterministic device verification. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageDebugEntryPoint {
    fun images(): ImageRepository
}

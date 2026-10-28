package com.parka.mdive.di

import android.content.ContentResolver
import android.content.Context
import com.parka.mdive.data.document.ContentDocumentRepository
import com.parka.mdive.domain.document.DocumentRepository
import com.parka.mdive.data.document.CommonMarkParser
import com.parka.mdive.domain.document.MarkdownParser
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentModule {
    @Binds abstract fun bindMarkdownParser(parser: CommonMarkParser): MarkdownParser

    @Binds abstract fun bindDocumentRepository(repository: ContentDocumentRepository): DocumentRepository

    companion object {
        @Provides fun contentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver
    }
}

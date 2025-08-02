package com.ishdemon.serverapp.di

import com.ishdemon.common.CryptoUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {
    @Provides
    fun provideCryptoUtils(): CryptoUtils = CryptoUtils()
}

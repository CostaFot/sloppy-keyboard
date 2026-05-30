package com.feelsokman.androidtemplate.keyboard

import android.app.Application
import com.feelsokman.androidtemplate.keyboard.first.FirstViewModel
import com.feelsokman.androidtemplate.keyboard.second.SecondViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface KeyboardEntryPoint {
    fun firstScreenViewModel(): FirstViewModel
    fun secondScreenViewModel(): SecondViewModel
}

fun Application.keyboardEntryPoint() =
    EntryPointAccessors.fromApplication(this, KeyboardEntryPoint::class.java)
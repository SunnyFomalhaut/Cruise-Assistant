package com.cruise.assistant.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** 无 DI 框架下的轻量工厂 */
@Suppress("UNCHECKED_CAST")
fun <T : ViewModel> viewModelFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }

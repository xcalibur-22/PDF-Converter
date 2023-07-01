package com.example.pdfconverter

import android.graphics.Bitmap

data class State(
    val imageBitmaps : List<Bitmap> = emptyList(),
    val isLoading : Boolean = false,
    val success : Boolean? = null
)
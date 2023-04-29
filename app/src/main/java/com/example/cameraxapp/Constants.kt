package com.example.cameraxapp

object Constants {
    const val TAG = "AppLog"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-sss"
    const val REQUEST_CODE_PERMISSION = 101
    val REQUIRED_PERMISSION = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
    )
}
package com.yourpackagename.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun File.toSecureUri(context: Context): Uri {
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        this
    )
}

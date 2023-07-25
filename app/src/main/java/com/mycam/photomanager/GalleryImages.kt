package com.mycam.photomanager

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore

class GalleryImages {
    fun listOfImages(context: Context) : ArrayList<String> {
        val listOfAllImages: ArrayList<String> = ArrayList()
        var absolutePathOfImage: String
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA,MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val orderBy = MediaStore.Images.Media.DATE_TAKEN

        val cursor: Cursor = context.contentResolver.query(uri,projection,null,null,"$orderBy DESC")!!

        val columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        //val columnIndexFolderName = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        while (cursor.moveToNext()) {
            absolutePathOfImage = cursor.getString(columnIndexData)

            listOfAllImages.add(absolutePathOfImage)
        }

        return listOfAllImages
    }
}
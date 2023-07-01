package com.example.pdfconverter

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.pdfconverter.util.ComposeFileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

@Composable
fun MainScreen(viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()){
    val context = LocalContext.current
    val file = context.createImageFile()
    var hasImage by remember {
        mutableStateOf(false)
    }
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        BuildConfig.APPLICATION_ID + ".provider", file
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = {
            viewModel.onImagesSelected(it, context)
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument(),
        onResult = {
            if (it != null) {
                viewModel.writeToSelectedPath(it, context)
            }
        }
    )
     val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            viewModel.onImagesSelected(mutableListOf(uri), context)
        }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
    val state = viewModel.state.collectAsState()
    val imageBitmaps = state.value.imageBitmaps
    val isLoading = state.value.isLoading
    val success = state.value.success

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    LaunchedEffect(key1 = success ) {
        if (success != null){
            if (success) {
                Toast.makeText(
                    context,
                    "Successfully converted images to pdf",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Something went wrong",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        if (imageBitmaps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f),
                verticalArrangement = Arrangement.spacedBy(8.dp),

                ) {
                itemsIndexed(imageBitmaps) { index: Int, bitmap: Bitmap ->
                    ImagePreviewItem(
                        bitmap = bitmap,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 15.dp),
                        onRemoveClick = {
                            viewModel.removeImage(index)
                        }
                    )
                }
            }
        } else Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.75f),
            contentAlignment = Alignment.Center
        ){
            Text(text = "Select some images")
        }

        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()) {

                Button(
                    modifier=Modifier.align(Alignment.Center),
                    colors=ButtonDefaults.buttonColors(
                      backgroundColor = Color.Green,
                      contentColor = Color.White
                    ),
                    enabled = imageBitmaps.isNotEmpty(),
                    onClick = { createDocumentLauncher.launch("PdfConverter_${System.currentTimeMillis()}.pdf") }
                ) {
                    Text("Save")
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .align(alignment = Alignment.BottomEnd),
                        onClick = { galleryLauncher.launch("image/*") },
                        backgroundColor = MaterialTheme.colors.surface

                    ) {
                        Icon(imageVector = Icons.Filled.Image, contentDescription = "Add")
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        FloatingActionButton(
                            modifier = Modifier
                                .padding(all = 16.dp)
                                .align(alignment = Alignment.BottomStart),
                            onClick = {
                                val permissionCheckResult =
                                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(uri)
                                } else {
                                    // Request a permission
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            backgroundColor = MaterialTheme.colors.surface

                        ) {
                            Icon(imageVector = Icons.Default.Camera, contentDescription = "Add")
                        }
            }
        }
    }
    if (isLoading){
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            AlertDialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                buttons = {}
            )
            CircularProgressIndicator()
        }
    }
}}
@Composable
fun ImagePreviewItem(bitmap: Bitmap, onRemoveClick: () -> Unit,modifier: Modifier) {
    Card(shape = RoundedCornerShape(8.dp),
        elevation = 8.dp
        ) {


        Column(
        ) {
            AsyncImage(
                model = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit,
            )
            Button(
                onClick = onRemoveClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red,
                    contentColor = Color.White
                ),

                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )

            }
        }
    }
}
fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}
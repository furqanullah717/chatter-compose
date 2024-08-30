package com.codewithfk.chatter.feature.chat

import android.Manifest
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.codewithfk.chatter.R
import com.codewithfk.chatter.feature.home.ChannelItem
import com.codewithfk.chatter.model.Message
import com.codewithfk.chatter.ui.theme.DarkGrey
import com.codewithfk.chatter.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import org.jetbrains.annotations.Async
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String) {
    Scaffold(
        containerColor = Color.Black
    ) {
        val viewModel: ChatViewModel = hiltViewModel()
        val chooserDialog = remember {
            mutableStateOf(false)
        }

        val cameraImageUri = remember {
            mutableStateOf<Uri?>(null)
        }

        val cameraImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success) {
                cameraImageUri.value?.let {
                    viewModel.sendImageMessage(it, channelId)
                }
            }
        }

        val imageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.sendImageMessage(it, channelId) }
        }


        fun createImageUri(): Uri {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = ContextCompat.getExternalFilesDirs(
                navController.context, Environment.DIRECTORY_PICTURES
            ).first()
            return FileProvider.getUriForFile(navController.context,
                "${navController.context.packageName}.provider",
                File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                    cameraImageUri.value = Uri.fromFile(this)
                })
        }

        val permissionLauncher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    cameraImageLauncher.launch(createImageUri())
                }
            }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            LaunchedEffect(key1 = true) {
                viewModel.listenForMessages(channelId)
            }
            val messages = viewModel.message.collectAsState()
            ChatMessages(
                messages = messages.value,
                onSendMessage = { message ->
                    viewModel.sendMessage(channelId, message)
                },
                onImageClicked = {
                    chooserDialog.value = true
                },
                channelName = channelName,
                viewModel = viewModel,
                channelID = channelId
            )
        }

        if (chooserDialog.value) {
            ContentSelectionDialog(onCameraSelected = {
                chooserDialog.value = false
                if (navController.context.checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    cameraImageLauncher.launch(createImageUri())
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }, onGallerySelected = {
                chooserDialog.value = false
                imageLauncher.launch("image/*")
            })
        }
    }
}


@Composable
fun ContentSelectionDialog(onCameraSelected: () -> Unit, onGallerySelected: () -> Unit) {
    AlertDialog(onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = onCameraSelected) {
                Text(text = "Camera")
            }
        },
        dismissButton = {
            TextButton(onClick = onGallerySelected) {
                Text(text = "Gallery")
            }
        },
        title = { Text(text = "Select your source?") },
        text = { Text(text = "Would you like to pick an image from the gallery or use the") })
}

@Composable
fun ChatMessages(
    channelName: String,
    channelID: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onImageClicked: () -> Unit,
    viewModel: ChatViewModel
) {
    val hideKeyboardController = LocalSoftwareKeyboardController.current

    val msg = remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                ChannelItem(channelName = channelName, Modifier, true, onClick = {}, onCall = { callButton->
                    viewModel.getAllUserEmails(channelID) {
                        val list: MutableList<ZegoUIKitUser> = mutableListOf()
                        it.forEach { email ->
                            Firebase.auth.currentUser?.email?.let { em ->
                                if(email != em){
                                    list.add(
                                        ZegoUIKitUser(
                                            email, email
                                        )
                                    )
                                }
                            }
                        }
                        callButton.setInvitees(list)
                    }
                })
            }
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGrey)
                .padding(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                msg.value = ""
                onImageClicked()
            }) {
                Image(
                    painter = painterResource(id = R.drawable.attach), contentDescription = "send"
                )
            }

            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Type a message") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    hideKeyboardController?.hide()
                }),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = DarkGrey,
                    unfocusedContainerColor = DarkGrey,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedPlaceholderColor = Color.White,
                    unfocusedPlaceholderColor = Color.White
                )
            )
            IconButton(onClick = {
                onSendMessage(msg.value)
                msg.value = ""
            }) {
                Image(painter = painterResource(id = R.drawable.send), contentDescription = "send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        Purple
    } else {
        DarkGrey
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)

    ) {
        val alignment = if (!isCurrentUser) Alignment.CenterStart else Alignment.CenterEnd
        Row(
            modifier = Modifier
                .padding(8.dp)
                .align(alignment),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isCurrentUser) {
                Image(
                    painter = painterResource(id = R.drawable.friend),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Box(
                modifier = Modifier
                    .background(
                        color = bubbleColor, shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                if (message.imageUrl != null) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(text = message.message?.trim() ?: "", color = Color.White)
                }
            }

        }

    }
}

@Composable
fun CallButton(isVideoCall: Boolean, onClick: (ZegoSendCallInvitationButton) -> Unit) {
    AndroidView(factory = { context ->
        val button = ZegoSendCallInvitationButton(context)
        button.setIsVideoCall(isVideoCall)
        button.resourceID = "zego_data"
        button
    }, modifier = Modifier.size(50.dp)) { zegoCallButton ->
        zegoCallButton.setOnClickListener { _ -> onClick(zegoCallButton) }
    }
}


package com.codewithfk.chatter.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.codewithfk.chatter.AppID
import com.codewithfk.chatter.AppSign
import com.codewithfk.chatter.MainActivity
import com.codewithfk.chatter.feature.chat.CallButton
import com.codewithfk.chatter.ui.theme.DarkGrey
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current as MainActivity
    LaunchedEffect(Unit) {
        Firebase.auth.currentUser?.let {
            context.initZegoService(
                appID = AppID,
                appSign = AppSign,
                userID = it.email!!,
                userName = it.email!!
            )
        }
    }
    val viewModel = hiltViewModel<HomeViewModel>()
    val channels = viewModel.channels.collectAsState()
    val addChannel = remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState()
    Scaffold(
        floatingActionButton = {
            Box(modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Blue)
                .clickable {
                    addChannel.value = true
                }) {
                Text(
                    text = "Add Channel", modifier = Modifier.padding(16.dp), color = Color.White
                )
            }
        }, containerColor = Color.Black
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn {
                item {
                    Text(
                        text = "Messages",
                        color = Color.Gray,
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black),
                        modifier = Modifier.padding(16.dp)
                    )
                }

                item {
                    TextField(value = "",
                        onValueChange = {},
                        placeholder = { Text(text = "Search...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(
                                RoundedCornerShape(40.dp)
                            ),
                        textStyle = TextStyle(color = Color.LightGray),
                        colors = TextFieldDefaults.colors().copy(
                            focusedContainerColor = DarkGrey,
                            unfocusedContainerColor = DarkGrey,
                            focusedTextColor = Color.Gray,
                            unfocusedTextColor = Color.Gray,
                            focusedPlaceholderColor = Color.Gray,
                            unfocusedPlaceholderColor = Color.Gray,
                            focusedIndicatorColor = Color.Gray
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search, contentDescription = null
                            )
                        })
                }

                items(channels.value) { channel ->
                    Column {
                        ChannelItem(
                            channelName = channel.name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            false,
                            onClick = {
                                navController.navigate("chat/${channel.id}&${channel.name}")
                            },
                            onCall = {})
                    }
                }
            }
        }
    }

    if (addChannel.value) {
        ModalBottomSheet(onDismissRequest = { addChannel.value = false }, sheetState = sheetState) {
            AddChannelDialog {
                viewModel.addChannel(it)
                addChannel.value = false
            }
        }
    }

}


@Composable
fun ChannelItem(
    channelName: String,
    modifier: Modifier,
    shouldShowCallButtons: Boolean = false,
    onClick: () -> Unit,
    onCall: (ZegoSendCallInvitationButton) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkGrey)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable {
                    onClick()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.Yellow.copy(alpha = 0.3f))

            ) {
                Text(
                    text = channelName[0].uppercase(),
                    color = Color.White,
                    style = TextStyle(fontSize = 35.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }


            Text(text = channelName, modifier = Modifier.padding(8.dp), color = Color.White)
        }
        if (shouldShowCallButtons) {
            Row(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                CallButton(isVideoCall = true, onCall)
                CallButton(isVideoCall = false, onCall)
            }
        }
    }
}

@Composable
fun AddChannelDialog(onAddChannel: (String) -> Unit) {
    val channelName = remember {
        mutableStateOf("")
    }
    Column(
        modifier = Modifier.padding(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add Channel")
        Spacer(modifier = Modifier.padding(8.dp))
        TextField(value = channelName.value, onValueChange = {
            channelName.value = it
        }, label = { Text(text = "Channel Name") }, singleLine = true)
        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = { onAddChannel(channelName.value) }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Add")
        }
    }
}
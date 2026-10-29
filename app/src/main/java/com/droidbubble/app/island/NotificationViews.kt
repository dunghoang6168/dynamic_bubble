package com.droidbubble.app.island

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidbubble.app.model.NotificationInfo
import java.util.Date

private val SecondaryText = Color(0xFFA1A1AA)

/** Fires a notification's intent; Android 14+ needs explicit leave to start an activity from the overlay (e.g. answering a call). */
fun sendFromIsland(context: Context, intent: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val options = ActivityOptions.makeBasic().apply {
            pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }.toBundle()
        intent.send(context, 0, null, null, null, null, options)
    } else {
        intent.send()
    }
}

/** What the island shows while the lock screen hides notification content: the app only, like iOS previews "When Unlocked". */
fun NotificationInfo.redacted(): NotificationInfo = if (isCall) this else copy(
    title = appName,
    text = "Notification",
    sender = null,
    iconBitmap = appIcon,
    image = null,
    actions = emptyList()
)

/** "Sender: text" with the sender dimmed; just the text for 1:1 chats and plain notifications. */
fun notificationLine(notification: NotificationInfo): AnnotatedString = buildAnnotatedString {
    notification.sender?.let { sender ->
        withStyle(SpanStyle(color = SecondaryText)) { append("$sender: ") }
    }
    append(notification.text)
}

@Composable
fun NotificationAvatar(bitmap: Bitmap?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color(0xFFE5E5EA),
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

@Composable
fun notificationTime(notification: NotificationInfo): String {
    val context = LocalContext.current
    return remember(notification.timestamp) { DateFormat.getTimeFormat(context).format(Date(notification.timestamp)) }
}

/**
 * Content of the popup that blooms out of the pill for a new notification. The first row has the
 * pill's height and keeps its centre clear so the camera cutout sits between app name and time.
 */
@Composable
fun NotificationPopupContent(notification: NotificationInfo, headerHeight: Dp) {
    val timer = remember(notification.key, notification.timestamp) { Animatable(1f) }
    LaunchedEffect(timer) {
        timer.animateTo(0f, tween(IslandStateManager.POP_IN_DURATION_MS.toInt(), easing = LinearEasing))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (notification.appIcon != null) {
                    Image(
                        bitmap = notification.appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                    )
                }
                Text(
                    text = notification.appName,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Camera cutout
            Spacer(modifier = Modifier.width(36.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(text = notificationTime(notification), color = SecondaryText, fontSize = 12.sp, maxLines = 1)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 14.dp, end = 14.dp, top = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            NotificationAvatar(notification.iconBitmap, 36.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = notificationLine(notification),
                    color = Color(0xFFD1D1D6),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFF1C1C1E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(timer.value.coerceIn(0f, 1f))
                    .background(Color(0xFF636366))
            )
        }
    }
}

/** A collapsed chat in the expanded card: tap it to focus it and reveal its picture and actions. */
@Composable
fun ConversationRow(notification: NotificationInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotificationAvatar(notification.iconBitmap, 36.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = notificationTime(notification), color = SecondaryText, fontSize = 12.sp)
            }
            Text(
                text = notificationLine(notification),
                color = Color(0xFFD1D1D6),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

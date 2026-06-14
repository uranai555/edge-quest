package com.edgequest.hero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgequest.hero.ui.theme.DarkNavy
import com.edgequest.hero.ui.theme.DarkNavyCard
import com.edgequest.hero.ui.theme.Emerald
import com.edgequest.hero.ui.theme.Gold
import com.edgequest.hero.ui.theme.SubText

@Composable
fun PermissionScreen(
    onOpenOverlaySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkNavy
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "画面端クエスト",
                style = MaterialTheme.typography.headlineLarge,
                color = Gold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Edge Quest",
                style = MaterialTheme.typography.titleMedium,
                color = SubText
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 説明カード
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkNavyCard)
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "ユウを画面端に召喚する",
                        style = MaterialTheme.typography.titleLarge,
                        color = Emerald,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "スマホの画面端に小さな勇者を表示するために、\n「他のアプリの上に表示」権限を使います。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "通知の中身、画面内容、入力内容は読み取りません。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gold,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "表示はいつでもOFFにできます。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onOpenOverlaySettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald,
                    contentColor = DarkNavy
                )
            ) {
                Text(
                    text = "権限を許可して召喚する",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "あとで設定からOFFにできます",
                style = MaterialTheme.typography.bodySmall,
                color = SubText,
                textAlign = TextAlign.Center
            )
        }
    }
}
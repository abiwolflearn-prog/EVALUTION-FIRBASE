package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.WcuBlue100
import com.example.ui.theme.WcuGold500
import com.example.ui.theme.WcuNavy800
import com.example.ui.theme.WcuNavy900
import com.example.ui.theme.WcuWhite

/**
 * Section 1: Splash Screen.
 * Displayed while Firebase Authentication session and Firestore role verification are in progress.
 */
@Composable
fun WcuSplashScreen(
  statusMessage: String = "Verifying Firebase session & academic role...",
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(WcuNavy900)
      .padding(24.dp)
      .testTag("wcu_splash_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(84.dp)
          .clip(RoundedCornerShape(22.dp))
          .background(WcuNavy800)
          .border(2.dp, WcuGold500, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.School,
          contentDescription = "Wachemo University Crest",
          tint = WcuGold500,
          modifier = Modifier.size(44.dp),
        )
      }

      Surface(
        color = WcuNavy800,
        shape = RoundedCornerShape(999.dp),
      ) {
        Text(
          text = stringResource(R.string.campus_tag),
          style = MaterialTheme.typography.labelSmall,
          color = WcuGold500,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }

      Text(
        text = stringResource(R.string.university_name),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = WcuWhite,
        textAlign = TextAlign.Center,
      )

      Text(
        text = "Teacher Evaluation & Academic Quality Assurance System",
        style = MaterialTheme.typography.bodyMedium,
        color = WcuBlue100,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(12.dp))

      CircularProgressIndicator(
        color = WcuGold500,
        strokeWidth = 3.dp,
        modifier = Modifier.size(32.dp),
      )

      Text(
        text = statusMessage,
        style = MaterialTheme.typography.labelMedium,
        color = WcuBlue100,
        textAlign = TextAlign.Center,
      )
    }
  }
}

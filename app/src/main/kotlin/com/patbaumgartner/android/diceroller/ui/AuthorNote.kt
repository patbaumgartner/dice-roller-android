package com.patbaumgartner.android.diceroller.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patbaumgartner.android.diceroller.BuildConfig
import com.patbaumgartner.android.diceroller.R
import com.patbaumgartner.android.diceroller.ui.theme.DiceRollerTheme

@Composable
internal fun AuthorNote() {
    val uriHandler = LocalUriHandler.current
    val authorUrl = stringResource(R.string.about_author_url)
    val authorName = stringResource(R.string.about_author_name)
    val authorNote = stringResource(R.string.about_author)
    val authorStart = authorNote.indexOf(authorName)
    val styledAuthorNote = buildAnnotatedString {
        append(authorNote)
        if (authorStart >= 0) {
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), authorStart, authorStart + authorName.length)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = stringResource(R.string.about_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.about_app_license),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { uriHandler.openUri(authorUrl) }) {
            Text(styledAuthorNote, style = MaterialTheme.typography.bodyMedium, color = DiceRollerTheme.accents.brand)
        }
    }
}

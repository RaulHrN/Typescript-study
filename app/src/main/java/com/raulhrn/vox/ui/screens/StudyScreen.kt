package com.raulhrn.vox.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raulhrn.vox.core.Studies
import com.raulhrn.vox.ui.Header
import com.raulhrn.vox.ui.ListRow
import com.raulhrn.vox.ui.Nav
import com.raulhrn.vox.ui.Route
import com.raulhrn.vox.ui.ScreenPadding
import com.raulhrn.vox.ui.Vox
import com.raulhrn.vox.ui.VoxType

@Composable
fun StudyScreen(nav: Nav) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        item { Header("Estudar") }
        itemsIndexed(Studies.all) { i, a ->
            ListRow(a.title, a.summary) { nav.push(Route.Article(i)) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun ArticleScreen(nav: Nav, index: Int) {
    val c = Vox.colors
    val article = Studies.all.getOrNull(index)
    Column(Modifier.fillMaxSize().padding(horizontal = ScreenPadding)) {
        Header(article?.title ?: "Estudo", onBack = { nav.pop() }, subtitle = "Estudar")
        if (article == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(article.summary, style = VoxType.heading, color = c.muted)
            Spacer(Modifier.height(12.dp))
            for (line in article.body.lines()) {
                when {
                    line.isBlank() -> Spacer(Modifier.height(10.dp))
                    line.startsWith("# ") -> Text(
                        line.removePrefix("# "),
                        style = VoxType.heading,
                        color = c.text,
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                    )
                    line.startsWith("- ") -> Row(Modifier.padding(vertical = 4.dp)) {
                        Text("•", style = VoxType.body, color = c.accent, modifier = Modifier.width(18.dp))
                        Text(line.removePrefix("- "), style = VoxType.body, color = c.text)
                    }
                    else -> Text(line, style = VoxType.body, color = c.text)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

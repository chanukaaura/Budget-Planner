package com.lcktechnologies.budgetplanner.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.lcktechnologies.budgetplanner.data.AppState
import com.lcktechnologies.budgetplanner.data.Kind
import com.lcktechnologies.budgetplanner.data.r2
import com.lcktechnologies.budgetplanner.data.txInfo
import com.lcktechnologies.budgetplanner.i18n.I18n
import com.lcktechnologies.budgetplanner.ui.Fmt
import java.io.File
import java.io.FileOutputStream

private fun csvCell(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""

/** One row of exported data: date, type, category/source, method, amount, note. */
private fun rowFor(state: AppState, t: com.lcktechnologies.budgetplanner.data.Tx, lang: String): List<String> {
    val info = state.txInfo(t)
    val type = I18n.get(lang, info.typeKey)
    val category = if (t.kind == Kind.INCOME) "" else info.label
    val src = if (t.kind == Kind.INCOME) info.label else ""
    val method = if (t.method.isNotEmpty()) I18n.get(lang, "pm_" + t.method) else ""
    return listOf(t.date, type, category, src, method, t.amount.toString(), t.note)
}

/** Writes a CSV of every transaction to the public Downloads folder and shows a toast with the result. */
fun exportCsv(context: Context, state: AppState, fmt: Fmt) {
    val lang = state.appLang
    val header = listOf(
        I18n.get(lang, "date"), "Type", I18n.get(lang, "category"), I18n.get(lang, "source"),
        I18n.get(lang, "payment_method"), "Amount (${state.currency})", I18n.get(lang, "note_optional")
    )
    val rows = state.tx.sortedWith(compareBy({ it.date }, { it.ts }))
    val sb = StringBuilder()
    sb.append(header.joinToString(",") { csvCell(it) }).append("\r\n")
    rows.forEach { t -> sb.append(rowFor(state, t, lang).joinToString(",") { csvCell(it) }).append("\r\n") }
    if (rows.isEmpty()) sb.append(csvCell("No transactions yet")).append("\r\n")

    val filename = "budget-export-${System.currentTimeMillis()}.csv"
    val ok = saveToDownloads(context, filename, "text/csv", sb.toString().toByteArray(Charsets.UTF_8))
    Toast.makeText(
        context,
        if (ok) "Saved $filename to Downloads" else "Could not save the export",
        Toast.LENGTH_LONG
    ).show()
}

/** Builds a simple HTML report and hands it to Android's print framework, so the user can save it as a PDF. */
fun exportPdf(context: Context, state: AppState, fmt: Fmt) {
    val lang = state.appLang
    val rows = state.tx.sortedWith(compareByDescending<com.lcktechnologies.budgetplanner.data.Tx> { it.date }.thenByDescending { it.ts })
    val totalIn = r2(rows.filter { it.kind == Kind.INCOME }.sumOf { it.amount })
    val totalOut = r2(rows.filter { it.kind != Kind.INCOME }.sumOf { it.amount })

    val html = buildString {
        append("<html><head><meta charset='utf-8'><style>")
        append("body{font-family:sans-serif;padding:16px} table{width:100%;border-collapse:collapse;font-size:12px}")
        append("th,td{text-align:left;padding:4px 8px;border-bottom:1px solid #ddd} th{background:#f0f0f0}")
        append("</style></head><body>")
        append("<h2>Budget Planner export</h2>")
        append("<p>Total in ${fmt.money(totalIn)} &middot; Total out ${fmt.money(totalOut)}</p>")
        if (rows.isEmpty()) {
            append("<p>No transactions yet.</p>")
        } else {
            append("<table><thead><tr><th>Date</th><th>Type</th><th>Category / Source</th><th>Method</th><th>Amount</th><th>Note</th></tr></thead><tbody>")
            rows.forEach { t ->
                val r = rowFor(state, t, lang)
                val cat = if (r[2].isNotEmpty()) r[2] else r[3]
                append("<tr><td>${r[0]}</td><td>${r[1]}</td><td>$cat</td><td>${r[4]}</td><td>${fmt.money(t.amount)}</td><td>${r[6]}</td></tr>")
            }
            append("</tbody></table>")
        }
        append("</body></html>")
    }

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Budget_Export"
            val adapter = view.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
}

private fun saveToDownloads(context: Context, filename: String, mime: String, content: ByteArray): Boolean = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
            true
        } else false
    } else {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        FileOutputStream(File(dir, filename)).use { it.write(content) }
        true
    }
} catch (e: Exception) {
    false
}

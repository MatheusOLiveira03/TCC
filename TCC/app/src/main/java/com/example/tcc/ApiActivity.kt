package com.example.tcc

import android.app.Dialog
import android.content.Intent
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import com.example.tcc.api.ErroApi
import com.example.tcc.api.RetrofitClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class ApiActivity : BaseActivity() {
    protected val api get() = RetrofitClient.api(applicationContext)
    private val chamadas = mutableListOf<Call<*>>()
    private val dialogos = mutableListOf<Dialog>()

    protected fun aviso(texto: String) { Toast.makeText(this, texto, Toast.LENGTH_LONG).show() }
    protected fun sessaoValida(): Boolean {
        if (Sessao.token(this).isNotBlank()) return true
        entrarNovamente(); return false
    }
    protected fun entrarNovamente() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
    protected fun profissional() = Sessao.usuario(this)?.tipoUsuario == "PROFISSIONAL"
    protected fun pacienteId(): Long = if (!profissional()) Sessao.usuario(this)?.id ?: 0
        else intent.getLongExtra("paciente", 0)
    protected fun abrir(classe: Class<*>, paciente: Long = 0, meta: Long = 0) {
        startActivity(Intent(this, classe).putExtra("paciente", paciente).putExtra("meta", meta))
    }
    protected fun dialogo(builder: MaterialAlertDialogBuilder): AlertDialog =
        builder.create().also { d -> dialogos.add(d); d.setOnDismissListener { dialogos.remove(d) }; d.show() }
    protected fun formulario() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val p = (20 * resources.displayMetrics.density).toInt(); setPadding(p, p / 2, p, p / 2)
    }
    protected fun campo(pai: LinearLayout, rotulo: String, valor: String = "", multiline: Boolean = false) =
        EditText(this).apply {
            hint = rotulo; setText(valor)
            inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            if (multiline) minLines = 3
            pai.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
    protected fun textoHtml(texto: String?) = HtmlCompat.fromHtml(texto.orEmpty(), HtmlCompat.FROM_HTML_MODE_LEGACY)
    protected fun comoHtml(texto: String) = android.text.TextUtils.htmlEncode(texto).replace("\n", "<br>")

    // Reutiliza o estilo dos cartões/botões dos layouts existentes.
    protected fun <T : View> modelo(layout: Int, id: Int): T {
        val pagina = layoutInflater.inflate(layout, null, false)
        val view = pagina.findViewById<T>(id)
        (view.parent as ViewGroup).removeView(view)
        return view
    }
    protected fun removerModelos(vararg ids: Int): LinearLayout {
        val pai = findViewById<View>(ids[0]).parent as LinearLayout
        ids.forEach { id -> findViewById<View>(id)?.let { pai.removeView(it) } }
        return pai
    }
    protected fun <T> chamar(
        call: Call<T>, publica: Boolean = false, botao: Button? = null,
        aoErro: (ErroApi) -> Unit = { aviso(it.mensagem) }, sucesso: (T?) -> Unit
    ) {
        if (botao?.isEnabled == false) return
        val textoBotao = botao?.text
        botao?.isEnabled = false; botao?.text = "Aguarde..."
        val tokenInicial = Sessao.token(this)
        chamadas.add(call)
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, r: Response<T>) {
                chamadas.remove(call)
                if (call.isCanceled || isFinishing || isDestroyed) {
                    (r.body() as? java.io.Closeable)?.close(); r.errorBody()?.close(); return
                }
                botao?.isEnabled = true; botao?.text = textoBotao
                if (!publica && tokenInicial != Sessao.token(this@ApiActivity)) {
                    (r.body() as? java.io.Closeable)?.close(); r.errorBody()?.close(); return
                }
                if (r.isSuccessful) { sucesso(r.body()); return }
                val erro = ErroApi.de(r)
                if (r.code() == 401 && !publica) {
                    Sessao.limparSeToken(this@ApiActivity, tokenInicial)
                    entrarNovamente(); return
                }
                aoErro(erro)
            }
            override fun onFailure(call: Call<T>, e: Throwable) {
                chamadas.remove(call)
                if (call.isCanceled || isFinishing || isDestroyed) return
                botao?.isEnabled = true; botao?.text = textoBotao
                if (!publica && tokenInicial != Sessao.token(this@ApiActivity)) return
                aoErro(ErroApi(0, if (call.request().method == "GET")
                    "Não foi possível conectar à API. Tente novamente."
                    else "Não foi possível confirmar o envio. Atualize os dados antes de repetir."))
            }
        })
    }
    override fun onDestroy() {
        chamadas.toList().forEach { it.cancel() }; chamadas.clear()
        dialogos.toList().forEach { it.dismiss() }; dialogos.clear()
        super.onDestroy()
    }
}

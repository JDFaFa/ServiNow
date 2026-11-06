package com.example.servinow.features.cloud

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.*
import com.example.servinow.R
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInButton(enabled: Boolean, onToken: (String)->Unit, onError: ()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Button(onClick={
        busy=true
        scope.launch {
            try {
                val clientId=context.resources.getIdentifier("default_web_client_id","string",context.packageName)
                check(clientId!=0)
                val option=GetSignInWithGoogleOption.Builder(context.getString(clientId)).build()
                val response=CredentialManager.create(context).getCredential(context,GetCredentialRequest.Builder().addCredentialOption(option).build())
                val credential=response.credential
                check(credential is CustomCredential && credential.type==GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                onToken(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } catch (_: GetCredentialCancellationException) { /* User dismissed the account selector. */ }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { onError() }
            finally { busy=false }
        }
    }, enabled=enabled&&!busy, modifier=Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ui_continue_with_google_f56b))
    }
}

package com.feelsokman.androidtemplate.ui

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.feelsokman.androidtemplate.ui.ui.theme.AndroidTemplateComposeTheme
import com.feelsokman.design.theme.AppTheme

class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Column {

                        Spacer(modifier = Modifier.height(100.dp))
                        Button(
                            onClick = {
                                startActivity(
                                    Intent(this@SecondActivity, ThirdActivity::class.java),
                                    ActivityOptions.makeSceneTransitionAnimation(this@SecondActivity).toBundle()
                                )

                                finish()

                            }
                        ) {
                            Text(text = "click for third")
                        }
                    }
                }

            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidTemplateComposeTheme {
        Greeting("Android")
    }
}
package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TestNotificationTemplate(
    val appName: String,
    val title: String,
    val message: String
)

val SampleTestTemplates = listOf(
    TestNotificationTemplate(
        appName = "WhatsApp",
        title = "Mariana Silva",
        message = "Oi! Você conseguiu ver o relatório que te enviei ontem à noite?"
    ),
    TestNotificationTemplate(
        appName = "Nubank",
        title = "Compra aprovada de R$ 94,50",
        message = "Supermercado Pão de Açúcar - Cartão Virtual Final 8492"
    ),
    TestNotificationTemplate(
        appName = "Gmail",
        title = "Atualização de Projeto - Sprint 42",
        message = "A reunião de alinhamento técnico foi confirmada para as 15:00."
    ),
    TestNotificationTemplate(
        appName = "Instagram",
        title = "Novo seguidor e curtida",
        message = "marcos_dev curtiu sua foto e começou a te seguir."
    )
)

@Composable
fun TestNotificationDialog(
    onDismiss: () -> Unit,
    onSend: (appName: String, title: String, message: String) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var isCustom by remember { mutableStateOf(false) }

    var customAppName by remember { mutableStateOf("Meu App") }
    var customTitle by remember { mutableStateOf("Título de Teste") }
    var customMessage by remember { mutableStateOf("Esta é uma notificação disparada para teste de captura.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Disparar Notificação de Teste",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Escolha um modelo ou digite uma notificação customizada para verificar a captura em tempo real:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                SampleTestTemplates.forEachIndexed { index, template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedIndex = index
                                isCustom = false
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isCustom && selectedIndex == index)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !isCustom && selectedIndex == index,
                                onClick = {
                                    selectedIndex = index
                                    isCustom = false
                                }
                            )
                            Column(modifier = Modifier.padding(start = 6.dp)) {
                                Text(
                                    text = template.appName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Custom radio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCustom,
                        onClick = { isCustom = true }
                    )
                    Text(
                        text = "Personalizado",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                if (isCustom) {
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("Mensagem") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCustom) {
                        onSend(customAppName, customTitle, customMessage)
                    } else {
                        val t = SampleTestTemplates[selectedIndex]
                        onSend(t.appName, t.title, t.message)
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_trigger_test_notification"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Disparar Agora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

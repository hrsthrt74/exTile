package com.hrsthrt74.qstile.shizuku

import android.util.Log
import com.hrsthrt74.qstile.ICommandService

class CommandService : ICommandService.Stub() {
    companion object {
        private const val TAG = "CommandService"
    }

    override fun executeCommand(command: String?): String {
        return try {
            Log.d(TAG, "Executing command: $command")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val output = process.inputStream.bufferedReader().readText().trim()
            val error = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            Log.d(TAG, "Command output: $output")
            if (error.isNotEmpty()) {
                Log.e(TAG, "Command error: $error")
            }

            if (exitCode == 0) output else "ERROR: $error"
        } catch (e: Exception) {
            Log.e(TAG, "executeCommand failed", e)
            "ERROR: ${e.message}"
        }
    }
}

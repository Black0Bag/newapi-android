package com.black0bag.newapi.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * CopyUtils
 *
 * 全局复制工具：任何可复制文本统一走这里。
 * 用户需求（v0.4）：所有有效输出（日志、URL、key、PAT 等）都要有一键复制按钮。
 */
object CopyUtils {

    /**
     * 复制文本到剪贴板并弹出提示
     * @param context Context
     * @param label 复制内容的标签（如 "API URL"、"Token"）
     * @param content 要复制的文本
     */
    fun copy(context: Context, label: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, content))
        Toast.makeText(context, "$label 已复制", Toast.LENGTH_SHORT).show()
    }
}
package com.example.myapplication.ui.screen.execute

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.lib.utils.ISerialPortControl
import com.android.lib.utils.ISerialPortReadCallback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SerialPortClient(context: Context) {
    private val appContext = context.applicationContext
    private var serialPortService: ISerialPortControl? = null
    private val nmeaDataChannel = Channel<String>()

    // 用于接收串口数据的回调
    private val readCallback = object : ISerialPortReadCallback.Stub() {
        override fun onReadReceived(dev: Int, bytes: ByteArray, length: Int) {
            // 只关心RNSS模式下的数据
            if (dev == 2) { // RNSS模式的设备ID为2
                val data = String(bytes, 0, length)
                // 将数据发送到Channel，供Flow使用
                nmeaDataChannel.trySend(data)
            }
        }
    }

    // 用于绑定和解绑服务的连接对象
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            serialPortService = ISerialPortControl.Stub.asInterface(service)
            try {
                serialPortService?.registerCallback(appContext.packageName, readCallback)
                // 设备ID为2是RNSS模式
                val devId = 2
                serialPortService?.startRead(devId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serialPortService = null
        }
    }

    // 开始服务和数据接收
    fun start() {
        Intent("android.dev.SERIAL_PORT_SERVICE").also { intent ->
            intent.setPackage("com.intercom.service")
            appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // 停止服务和数据接收
    fun stop() {
        try {
            serialPortService?.unregisterCallback(appContext.packageName, readCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        appContext.unbindService(serviceConnection)
        nmeaDataChannel.close()
    }

    // 提供一个Flow来观察NMEA数据
    val nmeaDataFlow: Flow<String>
        get() = nmeaDataChannel.receiveAsFlow()
}

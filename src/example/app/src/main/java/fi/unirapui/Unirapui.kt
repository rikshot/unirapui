package fi.unirapui

class Unirapui {
    companion object {
        init {
            System.loadLibrary("unirapui")
        }

        @JvmStatic
        external fun start(port: Int)
    }
}
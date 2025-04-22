package nl.w8mr.kasmine

// Platform-independent ByteCodeWriter interface
expect class ByteCodeWriter() {
    operator fun String.unaryPlus()
    fun write(bytes: ByteArray)
    fun byte(value: Byte)
    fun ubyte(value: UByte)
    fun ushort(value: Int)
    fun ushort(value: UShort)
    fun short(value: Short)
    fun uint(value: UInt)
    fun instructionOneArgument(opcode: String, value: Int)
    fun instructionTwoArgument(opcode: String, value1: Int, value2: Int)
    fun toByteArray(): ByteArray
}
|                       | Other              | Integer         | Long            | Float           | Double          | Ref                | Byte/Bool      | Char           | Short          |
|-----------------------|--------------------|-----------------|-----------------|-----------------|-----------------|--------------------|----------------|----------------|----------------|
| Constant null         |                    |                 |                 |                 |                 | aconst_null (0x01) |                |                |                |
| Load local var index  |                    | iload (0x15)    | lload (0x16)    | fload (0x17)    | dload (0x18)    | aload (0x19)       |                |                |                |
| Load local var 0      |                    | iload_0 (0x1a)  | lload_0 (0x1e)  | fload_0 (0x22)  | dload_0 (0x26)  | aload_0 (0x2a)     |                |                |                |
| Load local var 1      |                    | iload_1 (0x1b)  | lload_1 (0x1f)  | fload_1 (0x23)  | dload_1 (0x27)  | aload_1 (0x2b)     |                |                |                |
| Load local var 2      |                    | iload_2 (0x1c)  | lload_2 (0x20)  | fload_2 (0x24)  | dload_2 (0x28)  | aload_2 (0x2c)     |                |                |                | 
| Load local var 3      |                    | iload_3 (0x1d)  | lload_3 (0x21)  | fload_3 (0x25)  | dload_3 (0x29)  | aload_3 (0x2d)     |                |                |                |
| Load from array       |                    | iaload (0x2e)   | laload (0x2f)   | faload (0x30)   | daload (0x31)   | aaload (0x32)      | baload (0x33)  | caload (0x34)  | saload (0x35)  |
| Store local var index |                    | istore (0x36)   | lstore (0x37)   | fstore (0x38)   | dstore (0x39)   | astore (0x3a)      |                |                |                |
| Store local var 0     |                    | istore_0 (0x3b) | lstore_0 (0x3f) | fstore_0 (0x43) | dstore_0 (0x47) | astore_0 (0x4b)    |                |                |                |
| Store local var 1     |                    | istore_1 (0x3c) | lstore_1 (0x40) | fstore_1 (0x44) | dstore_1 (0x48) | astore_1 (0x4c)    |                |                |                |
| Store local var 2     |                    | istore_2 (0x3d) | lstore_2 (0x41) | fstore_2 (0x45) | dstore_2 (0x49) | astore_2 (0x4d)    |                |                |                |
| Store local var 3     |                    | istore_3 (0x3e) | lstore_3 (0x42) | fstore_3 (0x46) | dstore_3 (0x4a) | astore_3 (0x4e)    |                |                |                |
| Store into array      |                    | iastore (0x4f)  | lastore (0x50)  | fastore (0x51)  | dastore (0x52)  | aastore (0x53)     | bastore (0x54) | castore (0x55) | sastore (0x56) |
| Pop                   |                    | pop (0x57)      | pop2 (0x58)     | pop (0x57)      | pop2 (0x58)     | pop (0x57)         |                |                |                |
| Pop 2                 |                    | pop2 (0x58)     |                 | pop2 (0x58)     |                 | pop2 (0x58)        |                |                |                |
| Dup                   |                    | dup (0x59)      |                 | dup (0x59)      |                 | dup (0x59)         |                |                |                |
| Add                   |                    | iadd (0x60)     | ladd (0x61)     | fadd (0x62)     | dadd (0x63)     |                    |                |                |                |
| Sub                   |                    | isub (0x64)     | lsub (0x65)     | fsub (0x66)     | dsub (0x67)     |                    |                |                |                |
| Mul                   |                    | imul (0x68)     | lmul (0x69)     | fmul (0x6a)     | dmul (0x6b)     |                    |                |                |                |
| return                |                    |                 |                 |                 |                 | areturn (0xb0)     |                |                |                |
| New array             |                    |                 |                 |                 |                 | anewarray (0xbd)   |                |                |                |
| Array length          | arraylength (0xbe) |                 |                 |                 |                 |                    |                |                |                |               
| Throw                 |                    |                 |                 |                 |                 | athrow (0xbf)      |                |                |                |

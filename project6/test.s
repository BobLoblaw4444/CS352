main:
 jal mj_main
 li $v0, 10
 syscall

minijavaNew:
 move $t0, $a0
 mul $a0, $a1, 4
 li $v0, 9
 syscall
 sw $t0, ($v0)
 j $ra

minijavaNewArray:
 move $t0, $a0
 mul $a0, $a0, 4
 add $a0, $a0, 4
 li $v0, 9
 syscall
 sw $t0, ($v0)
 j $ra

.data
.align 4
minijavaNewline:
 .asciiz "\n"

.text
minijavaPrint:
 li $v0, 1
 syscall
 la $a0, minijavaNewline
 li $v0, 4
 syscall
 j $ra

mj_main:
 add $sp, $sp, -4
 sw $fp, ($sp)
 move $fp, $sp
 add $sp, $sp, -16
 add $sp, $sp, -4
 sw $ra, ($sp)
 # a1: 0(1): NewObj *Fac :Fac
 sw $v0, -4($fp)
 sw $v1, -8($fp)
 sw $a0, -12($fp)
 la $a0, mj__v_Fac
 li $a1, 1
 jal minijavaNew
 move $a1, $v0
 lw $v0, -4($fp)
 lw $v1, -8($fp)
 lw $a0, -12($fp)
 # a0: 1(0): Int *10 :int
 li $a0, 10
 # a0: 2(0): Arg 1 *0 :int
 # a0: 3(0): Call 0 *ComputeFac(2) :int
 sw $v0, -4($fp)
 sw $v1, -8($fp)
 sw $a1, -12($fp)
 move $v0, $a1
 lw $v1, ($v0)
 lw $v1, 0($v1)
 jal $v1
 move $a0, $v0
 lw $v0, -4($fp)
 lw $v1, -8($fp)
 lw $a1, -12($fp)
 # a0: 4(0): Print 3 :void
 sw $v0, -4($fp)
 sw $v1, -8($fp)
 sw $a0, -12($fp)
 sw $a1, -16($fp)
 jal minijavaPrint
 lw $v0, -4($fp)
 lw $v1, -8($fp)
 lw $a0, -12($fp)
 lw $a1, -16($fp)
 .ret_mj_main:
 lw $ra, ($sp)
 add $sp, $sp, 4
 move $sp, $fp
 lw $fp, ($sp)
 add $sp, $sp, 4
 j $ra
.data
.align 4
mj__v_Fac:
 .word mj__m_Fac_ComputeFac
.text
mj__m_Fac_ComputeFac:
 add $sp, $sp, -4
 sw $fp, ($sp)
 move $fp, $sp
 add $sp, $sp, -20
 add $sp, $sp, -4
 sw $ra, ($sp)
 # a0: 5(0): Parameter *0 :int
 # a1: 6(1): VarAssg 5 *num :int
 move $a1, $a0
 # a0: 7(0): Null *Type(int) :int
 move $a0, $zero
 # a0: 8(0): Int *1 :int
 li $a0, 1
 # a0: 9(0): Lt 6 8 :boolean
 slt $a0, $a1, $a0 # a0: 10(0): NBranch 9 *lif_2011117821_else :void
 beq $a0, $zero, .lif_2011117821_else
 # a0: 11(0): Int *1 :int
 li $a0, 1
 # a0: 12(0): VarAssg 11 *num_aux :int
 # a1: 13(1): Goto *lif_2011117821_done :void
 j .lif_2011117821_done
 # a0: 14(0): Label *lif_2011117821_else :void
 .lif_2011117821_else:
 # a2: 15(2): This :Fac
 move $a2, $v0
 # a0: 16(0): Int *1 :int
 li $a0, 1
 # a0: 17(0): Minus 6 16 :int
 sub $a0, $a1, $a0
 # a0: 18(0): Arg 17 *0 :int
 # a0: 19(0): Call 15 *ComputeFac(18) :int
 sw $v0, -4($fp)
 sw $v1, -8($fp)
 sw $a1, -12($fp)
 sw $a2, -16($fp)
 move $v0, $a2
 lw $v1, ($v0)
 lw $v1, 0($v1)
 jal $v1
 move $a0, $v0
 lw $v0, -4($fp)
 lw $v1, -8($fp)
 lw $a1, -12($fp)
 lw $a2, -16($fp)
 # a0: 20(0): Mul 6 19 :int
 mul $a0, $a1, $a0
 # a0: 21(0): VarAssg 20 *num_aux :int
 # a1: 22(1): Label *lif_2011117821_done :void
 .lif_2011117821_done:
 # a0: 23(0): Unify 12 21 :int
 # a0: 24(0): Return 23 :void
 move $v0, $a0
 j .ret_mj__m_Fac_ComputeFac
 .ret_mj__m_Fac_ComputeFac:
 lw $ra, ($sp)
 add $sp, $sp, 4
 move $sp, $fp
 lw $fp, ($sp)
 add $sp, $sp, 4
 j $ra


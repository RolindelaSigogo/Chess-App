// file: ChessPiece.kt
package com.example.chessapplication.model

enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

enum class PieceColor {
    WHITE, BLACK
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor,
    var row: Int,
    var col: Int
) {
    fun copy(): ChessPiece = ChessPiece(type, color, row, col)

    fun getSymbol(): String {
        return when (type) {
            PieceType.KING -> "K"
            PieceType.QUEEN -> "Q"
            PieceType.ROOK -> "R"
            PieceType.BISHOP -> "B"
            PieceType.KNIGHT -> "N"
            PieceType.PAWN -> "P"
        }
    }
}
// file: ChessGame.kt
package com.example.chessapplication.model

import kotlin.math.abs
import kotlin.math.sign

class ChessGame {
    val board = Array(8) { arrayOfNulls<ChessPiece>(8) }
    var currentPlayer = PieceColor.WHITE
    private var selectedPiece: ChessPiece? = null

    init { setupBoard() }

    private fun setupBoard() {
        // Clear board
        for (r in 0..7) for (c in 0..7) board[r][c] = null

        // White pawns on row 1, black pawns on row 6
        for (i in 0..7) {
            board[1][i] = ChessPiece(PieceType.PAWN, PieceColor.WHITE, 1, i)
            board[6][i] = ChessPiece(PieceType.PAWN, PieceColor.BLACK, 6, i)
        }

        // Rooks
        board[0][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE, 0, 0)
        board[0][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE, 0, 7)
        board[7][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK, 7, 0)
        board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK, 7, 7)

        // Knights
        board[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE, 0, 1)
        board[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE, 0, 6)
        board[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK, 7, 1)
        board[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK, 7, 6)

        // Bishops
        board[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE, 0, 2)
        board[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE, 0, 5)
        board[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK, 7, 2)
        board[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK, 7, 5)

        // Queens
        board[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE, 0, 3)
        board[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK, 7, 3)

        // Kings
        board[0][4] = ChessPiece(PieceType.KING, PieceColor.WHITE, 0, 4)
        board[7][4] = ChessPiece(PieceType.KING, PieceColor.BLACK, 7, 4)
    }

    private fun isInside(row: Int, col: Int): Boolean {
        return row in 0..7 && col in 0..7
    }

    fun getLegalMoves(fromRow: Int, fromCol: Int): List<Pair<Int, Int>> {
        val moves = mutableListOf<Pair<Int, Int>>()
        val piece = board[fromRow][fromCol] ?: return moves

        for (r in 0..7) {
            for (c in 0..7) {
                if (isValidMove(fromRow, fromCol, r, c)) {
                    moves.add(Pair(r, c))
                }
            }
        }
        return moves
    }

    private fun isValidMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (!isInside(fromRow, fromCol) || !isInside(toRow, toCol)) return false

        val piece = board[fromRow][fromCol] ?: return false
        val target = board[toRow][toCol]

        // Cannot capture own piece
        if (target != null && target.color == piece.color) return false

        return when (piece.type) {
            PieceType.PAWN -> validatePawnMove(piece, fromRow, fromCol, toRow, toCol)
            PieceType.ROOK -> validateRookMove(fromRow, fromCol, toRow, toCol)
            PieceType.BISHOP -> validateBishopMove(fromRow, fromCol, toRow, toCol)
            PieceType.KNIGHT -> validateKnightMove(fromRow, fromCol, toRow, toCol)
            PieceType.QUEEN -> validateQueenMove(fromRow, fromCol, toRow, toCol)
            PieceType.KING -> validateKingMove(fromRow, fromCol, toRow, toCol)
        }
    }

    private fun validatePawnMove(piece: ChessPiece, fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val direction = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRow = if (piece.color == PieceColor.WHITE) 1 else 6
        val target = board[toRow][toCol]

        // Normal single move forward
        if (fromCol == toCol && target == null) {
            if (toRow - fromRow == direction) return true
            // Double move from start row
            if (fromRow == startRow && toRow - fromRow == 2 * direction) {
                val midRow = fromRow + direction
                return board[midRow][fromCol] == null
            }
        }

        // Capture diagonally
        if (abs(toCol - fromCol) == 1 && toRow - fromRow == direction && target != null && target.color != piece.color) {
            return true
        }

        return false
    }

    private fun validateRookMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        return (fromRow == toRow || fromCol == toCol) && isPathClear(fromRow, fromCol, toRow, toCol)
    }

    private fun validateBishopMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        return abs(toRow - fromRow) == abs(toCol - fromCol) && isPathClear(fromRow, fromCol, toRow, toCol)
    }

    private fun validateKnightMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val rowDiff = abs(toRow - fromRow)
        val colDiff = abs(toCol - fromCol)
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)
    }

    private fun validateQueenMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        return validateRookMove(fromRow, fromCol, toRow, toCol) || validateBishopMove(fromRow, fromCol, toRow, toCol)
    }

    private fun validateKingMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val rowDiff = abs(toRow - fromRow)
        val colDiff = abs(toCol - fromCol)
        return rowDiff <= 1 && colDiff <= 1
    }

    private fun isPathClear(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        val stepRow = (toRow - fromRow).sign
        val stepCol = (toCol - fromCol).sign

        var r = fromRow + stepRow
        var c = fromCol + stepCol

        while (r != toRow || c != toCol) {
            if (board[r][c] != null) return false
            r += stepRow
            c += stepCol
        }
        return true
    }

    fun makeMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) return false

        // Simulate move to check if king would be in check
        val movingPiece = board[fromRow][fromCol] ?: return false
        val capturedPiece = board[toRow][toCol]

        // Make the move temporarily
        board[toRow][toCol] = movingPiece.copy().apply {
            row = toRow
            col = toCol
        }
        board[fromRow][fromCol] = null

        // Check if current player's king is in check after move
        val kingInCheck = isInCheck(currentPlayer)

        if (kingInCheck) {
            // Undo move - illegal because king would be in check
            board[fromRow][fromCol] = movingPiece
            board[toRow][toCol] = capturedPiece
            return false
        }

        // Move is legal, update piece position
        movingPiece.row = toRow
        movingPiece.col = toCol

        // Switch player
        currentPlayer = if (currentPlayer == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        return true
    }

    fun isInCheck(color: PieceColor): Boolean {
        val kingPos = findKing(color) ?: return false

        // Check if any opponent piece can capture the king
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color != color) {
                    if (isValidMove(r, c, kingPos.first, kingPos.second)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun isCheckmate(color: PieceColor): Boolean {
        if (!isInCheck(color)) return false

        // Check if any move can get out of check
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == color) {
                    val legalMoves = getLegalMoves(r, c)
                    for ((toR, toC) in legalMoves) {
                        // Try the move
                        val movingPiece = board[r][c]
                        val capturedPiece = board[toR][toC]

                        board[toR][toC] = movingPiece?.copy()?.apply {
                            row = toR
                            col = toC
                        }
                        board[r][c] = null

                        val stillInCheck = isInCheck(color)

                        // Undo move
                        board[r][c] = movingPiece
                        board[toR][toC] = capturedPiece

                        if (!stillInCheck) return false
                    }
                }
            }
        }
        return true
    }

    fun isStalemate(color: PieceColor): Boolean {
        if (isInCheck(color)) return false

        // Check if any legal moves exist
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.color == color) {
                    if (getLegalMoves(r, c).isNotEmpty()) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun findKing(color: PieceColor): Pair<Int, Int>? {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return Pair(r, c)
                }
            }
        }
        return null
    }

    fun resetGame() {
        setupBoard()
        currentPlayer = PieceColor.WHITE
    }
}
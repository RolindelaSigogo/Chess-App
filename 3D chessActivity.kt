package com.example.chessapplication

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.light.DirectionalLight
import io.github.sceneview.math.Position
import com.example.momoapp.model.ChessGame
import com.example.momoapp.model.PieceColor
import com.example.momoapp.model.ChessPiece
import io.github.sceneview.hitTest.RaycastHit

class Chess3DActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private val chessGame = ChessGame()

    // Map board coords to the ModelNode representing the piece (if any)
    private val pieceNodeAt = Array(8) { arrayOfNulls<ModelNode>(8) }
    // Indicator nodes for highlighting legal moves
    private val highlightNodes = mutableListOf<Node>()

    // Selected piece coords, or null
    private var selectedFrom: Pair<Int, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sceneView = SceneView(this)
        setContentView(sceneView)

        setupLights()
        createBoardSquares()
        loadPiecesIntoScene()

        // Touch handling
        sceneView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                handleTap(event.x, event.y)
            }
            true
        }
    }

    private fun setupLights() {
        val light = DirectionalLight(intensity = 10_000f)
        light.rotation = io.github.sceneview.math.Rotation(x = -45f, y = 45f)
        sceneView.addChild(light)
    }

    private fun createBoardSquares() {
        // create flat nodes to represent squares (so we can compute coordinates & optionally change material)
        for (r in 0..7) for (c in 0..7) {
            val square = Node().apply {
                position = Position(r.toFloat(), 0f, c.toFloat())
                name = "square_${r}_${c}"
                // optionally set a visual plane here (omitted for brevity)
            }
            sceneView.addChild(square)
        }
    }

    private fun getModelForPiece(type: String, color: com.example.momoapp.model.PieceColor): String {
        return if (color == PieceColor.WHITE) "white_$type" else "black_$type"
    }

    private fun loadPiecesIntoScene() {
        // clear any existing
        for (r in 0..7) for (c in 0..7) pieceNodeAt[r][c] = null

        for (r in 0..7) for (c in 0..7) {
            val piece = chessGame.board[r][c] ?: continue
            val modelName = getModelForPiece(piece.type.name.lowercase(), piece.color)
            val node = ModelNode(
                context = this,
                glbFileLocation = "models/$modelName.glb",
                scaleToUnits = 0.8f
            ).apply {
                position = Position(r.toFloat(), 0.3f, c.toFloat())
                // Name nodes so we can map back to board coords on pick
                name = "piece_${r}_${c}"
                // Store coordinates on node properties if you prefer
            }
            sceneView.addChild(node)
            pieceNodeAt[r][c] = node
        }
    }

    private fun handleTap(x: Float, y: Float) {
        // Perform a pick / raycast in SceneView. SceneView's API returns RaycastHits.
        val hits: List<RaycastHit> = try {
            sceneView.hitTest(x, y)
        } catch (e: Exception) {
            emptyList()
        }

        if (hits.isEmpty()) {
            // tapping empty space clears selection
            clearSelection()
            return
        }

        // Prefer piece hit first, then square
        val hitNode = hits.mapNotNull { it.node }.firstOrNull()
        if (hitNode == null) { clearSelection(); return }

        val name = hitNode.name ?: ""
        when {
            name.startsWith("piece_") -> {
                val coords = parseCoordsFromName(name, prefix = "piece_") ?: run { clearSelection(); return }
                onPieceTapped(coords.first, coords.second)
            }
            name.startsWith("square_") -> {
                val coords = parseCoordsFromName(name, prefix = "square_") ?: run { clearSelection(); return }
                onSquareTapped(coords.first, coords.second)
            }
            else -> {
                // If we hit a highlight node or something, treat as square click by projecting to nearest square position
                // Try to find nearest square by hit point
                val hit = hits.first()
                val point = hit.worldPosition
                val r = point.x.toInt().coerceIn(0,7)
                val c = point.z.toInt().coerceIn(0,7)
                onSquareTapped(r, c)
            }
        }
    }

    private fun parseCoordsFromName(name: String, prefix: String): Pair<Int, Int>? {
        val payload = name.removePrefix(prefix)
        val parts = payload.split("_")
        if (parts.size != 2) return null
        val r = parts[0].toIntOrNull() ?: return null
        val c = parts[1].toIntOrNull() ?: return null
        return Pair(r, c)
    }

    private fun onPieceTapped(r: Int, c: Int) {
        val piece = chessGame.board[r][c] ?: return
        val currentColor = chessGame.currentPlayer

        if (piece.color != currentColor) {
            // If a piece of the other color is tapped and we have a selected piece, attempt capture by tapping target
            val selected = selectedFrom
            if (selected != null) {
                val (fr, fc) = selected
                // ensure target is one of legal moves
                val legal = chessGame.getLegalMoves(fr, fc)
                if (Pair(r, c) in legal) {
                    executeMove(fr, fc, r, c)
                } else {
                    Toast.makeText(this, "Not a legal move", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "It's ${currentColor.name}'s turn", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // selecting own piece: highlight legal moves
        selectPiece(r, c)
    }

    private fun onSquareTapped(r: Int, c: Int) {
        val selected = selectedFrom
        if (selected == null) {
            // nothing selected — maybe user taps empty square: ignore or optionally place a hint
            return
        }
        val (fr, fc) = selected
        val legal = chessGame.getLegalMoves(fr, fc)
        if (Pair(r, c) in legal) {
            executeMove(fr, fc, r, c)
        } else {
            // tapped outside legal moves — clear selection or select piece at square if any
            if (chessGame.board[r][c] != null && chessGame.board[r][c]?.color == chessGame.currentPlayer) {
                selectPiece(r, c)
            } else {
                clearSelection()
            }
        }
    }

    private fun selectPiece(r: Int, c: Int) {
        clearHighlights()
        selectedFrom = Pair(r, c)
        // highlight the selected square (optional)
        addHighlightAt(r, c, selected = true)
        // highlight legal moves
        val legal = chessGame.getLegalMoves(r, c)
        legal.forEach { (tr, tc) -> addHighlightAt(tr, tc, selected = false) }
    }

    private fun clearSelection() {
        selectedFrom = null
        clearHighlights()
    }

    private fun clearHighlights() {
        highlightNodes.forEach { node ->
            sceneView.removeChild(node)
        }
        highlightNodes.clear()
    }

    private fun addHighlightAt(r: Int, c: Int, selected: Boolean) {
        // Add a small floating disk or node above the square as highlight.
        // You can have a small .glb or create a simple Node with a model/shape.
        val y = if (selected) 0.05f else 0.05f
        val node = Node().apply {
            position = Position(r.toFloat(), y, c.toFloat())
            // name for debug
            name = "highlight_${r}_${c}"
            // Optionally attach a small model or set material here.
        }
        sceneView.addChild(node)
        highlightNodes.add(node)
    }

    private fun executeMove(fromR: Int, fromC: Int, toR: Int, toC: Int) {
        // Remove captured node if present
        val targetNode = pieceNodeAt[toR][toC]
        if (targetNode != null) {
            sceneView.removeChild(targetNode)
            pieceNodeAt[toR][toC] = null
        }

        // Move the ModelNode of the piece
        val movingNode = pieceNodeAt[fromR][fromC] ?: run {
            // fallback: if no node loaded due to mismatch, still update logic board
            if (!chessGame.makeMove(fromR, fromC, toR, toC)) {
                Toast.makeText(this, "Move failed (illegal)", Toast.LENGTH_SHORT).show()
                clearSelection()
                return
            }
            postMoveChecks()
            return
        }

        // animate or immediately set position
        movingNode.position = Position(toR.toFloat(), 0.3f, toC.toFloat())
        // update node name and pieceNodeAt map
        movingNode.name = "piece_${toR}_${toC}"
        pieceNodeAt[toR][toC] = movingNode
        pieceNodeAt[fromR][fromC] = null

        // Update logic board
        val success = chessGame.makeMove(fromR, fromC, toR, toC)
        if (!success) {
            Toast.makeText(this, "Move failed (illegal)", Toast.LENGTH_SHORT).show()
            // rollback visual move (simple approach: move back)
            movingNode.position = Position(fromR.toFloat(), 0.3f, fromC.toFloat())
            movingNode.name = "piece_${fromR}_${fromC}"
            pieceNodeAt[fromR][fromC] = movingNode
            pieceNodeAt[toR][toC] = null
            clearSelection()
            return
        }

        // Clear selection & highlights
        clearSelection()

        // After move checks: check, checkmate, stalemate.
        postMoveChecks()
    }

    private fun postMoveChecks() {
        val opp = chessGame.currentPlayer // after a move, currentPlayer is the next player to move
        // If opponent is now in check
        if (chessGame.isInCheck(opp)) {
            // is it checkmate?
            if (chessGame.isCheckmate(opp)) {
                Toast.makeText(this, "Checkmate! ${if (opp == PieceColor.WHITE) "Black" else "White"} wins.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Check to ${opp.name}.", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If not in check, see if stalemate
            if (chessGame.isStalemate(opp)) {
                Toast.makeText(this, "Stalemate — draw.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sceneView.destroy()
    }
}

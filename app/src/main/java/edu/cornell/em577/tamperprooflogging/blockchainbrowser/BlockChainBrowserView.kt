package edu.cornell.em577.tamperprooflogging.blockchainbrowser

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import edu.cornell.em577.tamperprooflogging.data.model.BlockNode
import edu.cornell.em577.tamperprooflogging.data.source.BlockChainRepository
import java.util.*

class BlockChainBrowserView(context: Context, attributeSet: AttributeSet) :
    View(context, attributeSet) {

    /** A node in the directed acyclic graph of blocks that is fixed on a canvas */
    private data class CanvasNode(
        val block: BlockNode.Block,
        val point: Point,
        val parents: List<CanvasNode>
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Path()
    private val leftArrowEdge = Path()
    private val rightArrowEdge = Path()
    private var frontierNode: CanvasNode = updateFrontierNode()

    companion object {
        private const val nodeRadius = 15
        private const val edgeWidth = 2
    }

    private fun updateFrontierNode(): CanvasNode {
        // TODO: Implement blockchain coordinate selection
        val frontierBlock = BlockChainRepository.getBlockChain().block
        return CanvasNode(frontierBlock, Point(0,0), emptyList())
    }

    override fun onDraw(canvas: Canvas) {
        frontierNode = updateFrontierNode()
        drawGraph(canvas)
    }

    private fun drawGraph(canvas: Canvas) {
        drawNode(canvas, frontierNode.point)
        val queue = ArrayDeque(listOf(frontierNode))
        val visited = HashSet(listOf(frontierNode.block.cryptoHash))
        while (queue.isNotEmpty()) {
            val currentNode = queue.pop()
            for (parentNode in currentNode.parents) {
                if (parentNode.block.cryptoHash !in visited) {
                    drawNode(canvas, parentNode.point)
                    visited.add(parentNode.block.cryptoHash)
                    queue.addLast(parentNode)
                }
                drawEdge(canvas, currentNode.point, parentNode.point)
            }
        }
    }

    private fun drawNode(canvas: Canvas, point: Point) {
        paint.reset()
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), nodeRadius.toFloat(), paint)
    }

    private fun drawEdge(canvas: Canvas, src: Point, dest: Point) {
        // TODO: Implement arrows on edges
        paint.reset()
        edge.moveTo(src.x.toFloat(), src.y.toFloat())
        edge.lineTo(dest.x.toFloat(), dest.y.toFloat())

        leftArrowEdge.moveTo(dest.x.toFloat(), dest.y.toFloat())

        rightArrowEdge.moveTo(dest.x.toFloat(), dest.y.toFloat())

        paint.strokeWidth = edgeWidth.toFloat()
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawPath(edge, paint)
        canvas.drawPath(leftArrowEdge, paint)
        canvas.drawPath(rightArrowEdge, paint)
    }
}
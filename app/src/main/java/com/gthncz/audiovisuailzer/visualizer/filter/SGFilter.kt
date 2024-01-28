package com.gthncz.audiovisuailzer.visualizer.filter

import Jama.Matrix


/**
 * Savitzky-Golay Filter
 *
 * a algorithm of weighted sum in window.
 *
 * procedure:
 * 1. for every time point:
 * p_t = a_0 + a_1 * x_t + a_2 * x_t^2 + ... + a_{k-1} * x_t ^ {k-1}
 *
 * (a_0, a_1, a_2, ..., a_{k-1}) is the weight params,
 * p_t is predicted value,
 * x_t is observed value.
 *
 * 2. for the neighbor 2n time points:
 * [  p_{t-n} ]    [ 1 x_{t-n} x_{t-n}^2 ... x_{t-n}^{k-1} ] [ a_0 ]
 * [    ...   ]    [                    ...                ] [ a_1 ]
 * [  p_{t-1} ]    [ 1 x_{t-1} x_{t-1}^2 ... x_{t-1}^{k-1} ] [ a_2 ]
 * [   p_t    ] =  [ 1 x_{t}   x_{t}^2   ... x_{t}^{k-1}   ] [     ] + epsilon_{2n+1}
 * [  p_{t+1} ]    [ 1 x_{t+1} x_{t+1}^2 ... x_{t+1}^{k-1} ] [     ]
 * [    ...   ]    [                     ...               ] [     ]
 * [  p_{t+n} ]    [ 1 x_{t+n} x_{t+n}^2 ... x_{t+n}^{k-1} ] [ a_{k-1}]
 * in this way, $(2n+1) > k$ is required.
 *
 * the equation can be simplified to:
 * P_{2n+1}*1 = X_{2n+1}*k * A_k + E_{2n+1}*1
 *
 * 3. according to least squares method:
 * hat{A} = (X^{trans} * X)^{-1} *X^{trans} * P
 *
 * so,
 * hat{P} = X * A = X * (X^{trans} * X)^{-1} * X^{trans} * Y = B * P
 *
 * B = X * (X^{trans} * X)^{-1} * X^{trans}.
 *
 *
 */
class SGFilter(
    val windowSize: Int,
    val k: Int
): IDataFilter {

    private val step: Int
    private val matrixB: Matrix


    init {
        step = (windowSize - 1) / 2
        // initialize matrix X_{2n+1, k}.
        val matrixX = Matrix(windowSize, k)
        for (i in 0 until windowSize) {
            for (j in 0 until k) {
                matrixX.set(i, j, Math.pow(
                    (i - step).toDouble(), j.toDouble()
                ))
            }
        }
        // calculate matrix B.
        matrixB = matrixX.times(
            matrixX.transpose()
                .times(matrixX)
                .inverse()
        ).times(
            matrixX.transpose()
        )
    }

    override fun process(data: FloatArray) {
        val matrix = Matrix(windowSize, 1)
        data.forEachIndexed { index, fl ->
            for (i in 0 until step) {
                matrix.set(i, 0, data.getOrElse(i + index - step) {  fl }.toDouble())
            }
            matrix.set(step, 0, fl.toDouble())
            for (i in step+1 until windowSize) {
                matrix.set(i, 0, data.getOrElse(i + index - step) { fl }.toDouble())
            }
            data[index] = matrixB.times(matrix).get(step, 0).toFloat()
        }

    }


}
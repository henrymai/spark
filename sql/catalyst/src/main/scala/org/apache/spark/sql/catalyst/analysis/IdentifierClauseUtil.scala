/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.analysis

import org.apache.spark.sql.catalyst.expressions.Expression
import org.apache.spark.sql.types.StringType

/**
 * Resolves the catalog of the name parts for table/view/function/namespace.
 */
object IdentifierClauseUtil {
  private def getNotNullFoldableString(clauseName: String, expr: Expression): String = {
    expr match {
      case e if !e.foldable => expr.failAnalysis(
        errorClass = "NOT_A_CONSTANT_STRING.NOT_CONSTANT",
        messageParameters = Map(
          "name" -> clauseName,
          "expr" -> expr.sql))
      case e if e.dataType != StringType => expr.failAnalysis(
        errorClass = "NOT_A_CONSTANT_STRING.WRONG_TYPE",
        messageParameters = Map(
          "name" -> clauseName,
          "expr" -> expr.sql,
          "dataType" -> e.dataType.catalogString))
      case e =>
        e.eval() match {
          case null => expr.failAnalysis(
            errorClass = "NOT_A_CONSTANT_STRING.NULL",
            messageParameters = Map(
              "name" -> clauseName,
              "expr" -> expr.sql))
          case other => other.toString // OK
        }
    }
  }

  def evalIdentifierClause(expr: Expression): Seq[String] = {
    val str = getNotNullFoldableString("IDENTIFIER", expr)
    UnresolvedAttribute(str).nameParts
  }
}

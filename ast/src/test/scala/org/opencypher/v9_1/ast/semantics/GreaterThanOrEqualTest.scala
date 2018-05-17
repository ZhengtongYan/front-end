/*
 * Copyright © 2002-2018 Neo4j Sweden AB (http://neo4j.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.v9_1.ast.semantics

import org.opencypher.v9_0.util.DummyPosition
import org.opencypher.v9_0.util.symbols._
import org.opencypher.v9_1.expressions
import org.opencypher.v9_1.expressions.GreaterThanOrEqual

class GreaterThanOrEqualTest extends InfixExpressionTestBase(expressions.GreaterThanOrEqual(_, _)(DummyPosition(0))) {

  test("shouldSupportComparingIntegers") {
    testValidTypes(CTInteger, CTInteger)(CTBoolean)
  }

  test("shouldSupportComparingDoubles") {
    testValidTypes(CTFloat, CTFloat)(CTBoolean)
  }

  test("shouldSupportComparingStrings") {
    testValidTypes(CTString, CTString)(CTBoolean)
  }

  test("shouldSupportComparingPoints") {
    testValidTypes(CTPoint, CTPoint)(CTBoolean)
  }

  test("shouldSupportComparingTemporals") {
    testValidTypes(CTDate, CTDate)(CTBoolean)
    testValidTypes(CTTime, CTTime)(CTBoolean)
    testValidTypes(CTLocalTime, CTLocalTime)(CTBoolean)
    testValidTypes(CTDateTime, CTDateTime)(CTBoolean)
    testValidTypes(CTLocalDateTime, CTLocalDateTime)(CTBoolean)
  }

  test("shouldReturnErrorIfInvalidArgumentTypes") {
    testInvalidApplication(CTNode, CTInteger)("Type mismatch: expected Float, Integer, Point, String, Date, Time, LocalTime, LocalDateTime or DateTime but was Node")
    testInvalidApplication(CTInteger, CTNode)("Type mismatch: expected Float or Integer but was Node")
    testInvalidApplication(CTDuration, CTDuration)("Type mismatch: expected Float, Integer, Point, String, Date, Time, LocalTime, LocalDateTime or DateTime but was Duration")
  }
}

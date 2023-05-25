/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.mobiletaskscheduler.models

import play.api.libs.json.{Format, Json}

import java.time.Instant
import java.time.temporal.ChronoUnit

case class ScheduleRequest(
    minutes: Int,
    hours: Int,
    days: Int
) {
    def instant: Instant = {
        if (isNow) Instant.now()
        else
            Instant.now()
                   .plus(minutes, ChronoUnit.MINUTES)
                   .plus(hours, ChronoUnit.HOURS)
                   .plus(days, ChronoUnit.DAYS)
    }

    private def isNow: Boolean =
        minutes == 0 &&
            hours == 0 &&
            days == 0
}

object ScheduleRequest {
    implicit val format: Format[ScheduleRequest] = Json.format[ScheduleRequest]
}
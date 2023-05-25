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

package uk.gov.hmrc.mobiletaskscheduler.schedulers

import akka.actor.ActorSystem
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}
import uk.gov.hmrc.mobiletaskscheduler.services.ItemService

import javax.inject.Inject
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ItemScheduler @Inject()(
    actorSystem: ActorSystem,
    applicationLifecycle: ApplicationLifecycle,
    configuration: Configuration,
    itemService: ItemService
)(
    implicit ec: ExecutionContext
) {
    private val logger = Logger(getClass)

    private def execute(implicit ec: ExecutionContext): Future[Result] =
        itemService.scanAll.map(count => Result(s"Processed $count items"))

    private lazy val enabled: Boolean =
        configuration.get[Boolean]("scheduling.item.enabled")

    private lazy val initialDelay: FiniteDuration =
        configuration.get[FiniteDuration]("scheduling.item.initialDelay")

    private lazy val interval: FiniteDuration =
        configuration.get[FiniteDuration]("scheduling.item.interval")

    if (enabled) {
        logger.warn("ItemScheduler has started.")
        val cancellable =
            actorSystem.scheduler.scheduleAtFixedRate(initialDelay, interval) { () =>
                logger.info("Scheduled item job triggered")
                execute.onComplete {
                    case Success(Result(message)) =>
                        logger.info(s"Completed scheduled item job: $message")
                    case Failure(throwable) =>
                        logger.error(s"Exception running scheduled item job", throwable)
                }
            }
        applicationLifecycle.addStopHook(() => Future(cancellable.cancel()))
    } else
        logger.warn(s"ItemScheduler has been disabled.")

    case class Result(message: String)
}

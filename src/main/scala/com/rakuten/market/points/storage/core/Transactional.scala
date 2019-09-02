package com.rakuten.market.points.storage.core

import com.rakuten.market.points.storage.util.PostgresContext
import monix.eval.{Task, TaskLift}

object Transactional {


  implicit def monixQuill[F[_]: TaskLift](implicit ctx: PostgresContext): Transactional[Task, F] =
    new Transactional[Task, F] {
      override def transact[A](dbio: Task[A]): F[A] =
        TaskLift[F].apply(ctx.transaction(dbio))
    }

  object syntax {

    implicit class TransactionalSyntax[DBIO[_], A](val dbio: DBIO[A]) extends AnyVal {
      def transact[F[_]](implicit T: Transactional[DBIO, F]): F[A] =
        T.transact(dbio)
    }
  }
}

trait Transactional[DBIO[_], F[_]] {

  def transact[A](dbio: DBIO[A]): F[A]
}

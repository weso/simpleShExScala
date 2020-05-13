package es.weso.simpleshex
import cats.effect.IO

object Either2IO {

 def either2IO[A](either: Either[String,A]): IO[A] = 
    either.fold(
        s => IO.raiseError(new RuntimeException(s"Error: $s")), 
        IO.pure(_)
    )

}
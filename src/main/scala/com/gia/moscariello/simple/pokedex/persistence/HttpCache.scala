package com.gia.moscariello.simple.pokedex.persistence

import cats.effect.IO

trait HttpCache[Key, Value] {

  def get(k: Key): IO[Option[Value]]

  def put(k: Key, value: Value): IO[Unit]
}

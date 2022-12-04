package com.gia.moscariello.simple.pokedex.persistence

import cats.effect.IO
import com.github.blemale.scaffeine.{Cache, Scaffeine}

class InMemoryCache extends HttpCache[String, String] {
  private val cache: Cache[String, String] = Scaffeine()
    .recordStats()
    .build[String, String]()

  override def get(key: String): IO[Option[String]] = IO.pure(cache.getIfPresent(key))

  override def put(key: String, value: String): IO[Unit] = IO.pure(cache.put(key, value))
}

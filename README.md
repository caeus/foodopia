### USAGE

Install SBT

once installed

run `sbt run`

It's currently using in memory for users, but it can be easily replaced by any DB.

you can go to `localhost:9999/docs` to play with the api with Swagger.


### Technologies

The chosen language: Scala. It was done using a functional programming style with big regard
for testability (although no time for tests), referential transparency, immutability, 
separation of concerns and composability.

The chosen libraries: Http4s (One of scala's most purist Http server libraries), 
Sttp for connecting to other services,
Tapir for Swagger and rest layer and
ZIO for managing of effects (Like Javascript promises but Much better)
If I had had time to connect to DB I probably would have chosen Doobie, a Scala library that
plays really good with the previous libraries.

For the build system: SBT, as most Scala proyects

## Explanation of the code

Just go to the Main.scala file, it wires all dependencies manually (yep, no DI library)
and it will give you an overall view of the architecture.


## Caveats, obstacles and problems

Are there free APIs to search for cities and restaurants? I ended up paying a google service.

I was kind of a perfectionist and didn't have time to plug a DB nor pack everything into a docker.
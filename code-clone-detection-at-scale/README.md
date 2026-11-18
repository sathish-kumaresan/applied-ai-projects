# Code Clone Detection at Scale: Containerised Big Data Pipeline

**Applied Cloud Computing and Big Data, Blekinge Institute of Technology (BTH)**

A containerised pipeline that finds duplicated code across large Java systems from the Qualitas Corpus
(for example the full Eclipse SDK). The course provided a baseline clone detector, `cljDetector`, written in
Clojure with MongoDB. I instrumented it, made it faster, built a separate monitoring service, and analysed how it
behaves as the data grows.

## Pipeline

1. **Read and chunkify:** read every source file and split it into chunks of `CHUNKSIZE` lines
2. **Store:** store files and chunks in MongoDB with a hash per chunk
3. **Identify candidates:** group chunks by hash in a MongoDB aggregation to find duplicates
4. **Expand clones:** merge overlapping candidates into full clones, one candidate at a time

Three containers run together with Docker Compose: `mongo`, `cljdetector` and `monitor-tool`.

## My changes

- **Performance**
  - added a compound index on `fileName`, `startLine` and `endLine` for candidate instances, which made clone
    expansion much faster
  - replaced per-call database connections with one shared MongoDB connection
  - made chunk generation (`chunkify`) more efficient
- **Application monitoring** (`cljdetector/monitoring.clj`): timestamped timing of each stage (storing files,
  storing chunks, identifying candidates, expanding clones), JVM memory use, and average clone size after expansion
- **Monitoring service** (`monitor-tool`): a separate container that polls the MongoDB collections and reports
  their sizes while the pipeline runs

## Results on the Eclipse SDK

| CHUNKSIZE | Avg. chunks per file | Avg. clone size (lines) | Expand first 10k candidates | Expand last 10k candidates |
|---|---|---|---|---|
| 5 | 163.5 | 49.1 | 34.4 s | 104.2 s |
| 10 | 158.8 | 49.9 | 12.9 s | 44.3 s |
| 20 | 150.1 | 92.5 | 5.7 s | 24.8 s |
| 40 | 135.1 | 173.4 | 3.8 s | 10.4 s |

Main findings:
- Chunk generation time is constant per file, because each file is chunked independently.
- Candidate identification is a single aggregation over the chunks, so it doesn't depend on earlier candidates.
- Clone expansion slows down as candidates run out. The remaining candidates overlap less, so each one needs
  more individual work.
- Larger chunks give fewer, larger clones and much faster runs, but miss small duplicates.
- The full corpus did not fit on a laptop. Scaling further needs distributed processing, batching or larger
  machines.

## Run

```bash
docker volume create qc-volume        # load the Qualitas Corpus into this volume first
docker compose up --build
```

Set `CHUNKSIZE` and `SOURCEDIR` in `docker-compose.yaml`.

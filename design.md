# Artemis Continuity Plugin v2

## Ontology

### plugins

- ContinuityPlugin
  - bootstraps the continuity service
  - registers dependent plugins

- ContinuityMessageInterceptorPlugin
  - add UUID duplicate id
  - add Origin header to identify original source of message
  - add scheduled time of delivery for in.mirror to target queue bridged messages
  - remote divert for sync continuity - send a copy of the message to a remote destination (Phase II)
  - capture acks and pushing to target.out.acks queue

- ContinuityDestinationListener
  - when queue is added, delegate to the continuity service

### core

- ContinuityConfig
  - reads and parses settings from flat config properties
  - defines continuity configuration defaults
  - define which destinations should be managed for continuity for async or sync mode

- ContinuityService
  - single instance per broker
  - maintains status of live or backup
  - create command queue
  - manage session, producer, and consumer of command queue
  - evaluate if address/queue is subect to continuity
  - initialize and provide lookups of continuity flows
  - delegate continuity specific work to flow
  - capture address/queue info for commands/notifications

- ContinuityFlow
  - instance per queue subject to continuity
  - notified of queue changes
  - located and routed to by interceptors and listeners
  - create structure to manage the flow
    - create target.out.mirror queue
    - create target.out.acks queue
    - create target.in.mirror queue
    - create target.in.acks queue
    - create target if on remote site
    - create divert from target to target.out.mirror  
    - create remote bridge from target.out.mirror to target.in.mirror
    - create local bridge from target.in.mirror to target
    - create ContinuityAckDivert out.acks session/producer
    - create AckProducer in.acks session/consumer

- AckDivert
  - instance per queue subject to continuity
  - manages session/producer for captured acks
  - creates message with ack details

- AckManager
  - instance per queue subject to continuity
  - manage session/consumer for acks
  - remove duplicates from inbound staging queue
  - add dup ids to target queues
  - adjust delay for inbound staging bridge

- QueueInfo
- AckInfo

(ns rads.rsdp.modules.log-delivered
  "Algorithm 2.3: Log Delivered

  Module:
    - Name: LoggedPerfectPointToPointLinks, instance lpl.

  Events:
    - Request: ⟨ lpl, Send | q, m ⟩
      - Requests to send message m to process q.
    - Indication: ⟨ lpl, Deliver | delivered ⟩
      - Notifies the upper layer of potential updates to variable delivered in
        stable storage (which log-delivers messages according to the text).

  Properties:
    - LPL1: Reliable delivery
      - If a process that never crashes sends a message m to a correct process
        q, then q eventually log-delivers m.
    - LPL2: No duplication
      - No message is log-delivered by a process more than once.
    - LPL3: No creation
      - If some process q log-delivers a message m with sender p, then m was
        previously sent to q by process p.")

(ns rads.rsdp.modules.authenticate-and-filter
  "Algorithm 2.4: Authenticate and Filter
  
  Module:
    - Name: AuthPerfectPointToPointLinks, instance al.

  Events:
    - Request: ⟨ al, Send | q, m ⟩
      - Requests to send message m to process q.
    - Indication: ⟨ al, Deliver | p, m ⟩
      - Delivers message m sent by process p.

  Properties:
    - AL1: Reliable delivery
      - If a correct process sends a message m to a correct process q, then q
        eventually delivers m.
    - AL2: No duplication
      - No message is delivered by a correct process more than once.
    - AL3: Authenticity
      - If some correct process q delivers a message m with sender p and
        process p is correct, then m was previously sent to q by p.")

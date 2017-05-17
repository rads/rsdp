(ns rads.rsdp.modules.rotating-byzantine-leader-detection
  "Algorithm 2.10: Rotating Byzantine Leader Detection
  
  Module:
    - Name: ByzantineLeaderDetector, instance bld.

  Events:
    - Indication: ⟨ bld, Trust | p ⟩
      - Indicates that process p is trusted to be leader.
    - Request: ⟨ bld, Complain | p ⟩
      - Receives a complaint about process p.

  Properties:
    - BLD1: Eventual succession
      - If more than f correct processes that trust some pro- cess p complain
        about p, then every correct process eventually trusts a different process than p.
    - BLD2: Putsch resistance
      - A correct process does not trust a new leader unless at least one
        correct process has complained against the previous leader.
    - BLD3: Eventual agreement
      - There is a time after which no two correct processes trust different
        processes.")

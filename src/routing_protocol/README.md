# OSVP Routing Protocol Module – Developer README  
`src/routing_protocol/`

This directory contains the **core routing protocol implementation** of the **Open Scientific Validation Protocol (OSVP)** – a decentralized, peer-to-peer overlay designed to route validation requests, evidence packages, and reputation updates across a global network of scientific validation nodes.

If you are taking over development or want to extend/contribute to the routing layer, this README will get you up to speed quickly.

## Overview

The routing protocol is **content-addressed + reputation-aware** and inspired by a mix of:
- Kademlia (DHT-style XOR distance)
- BitTorrent Mainline DHT
- IPFS/libp2p bitswap ideas
- Custom reputation-weighted path selection

Key goals:
- Efficient discovery of nodes that can validate a specific claim (by domain, method, dataset, etc.)
- Resilient to sybils and malicious nodes via cryptographic reputation
- Low-latency forwarding of validation jobs and results
- Pluggable transport (currently QUIC + libp2p-style streams)

## Directory Structure

```
src/routing_protocol/
├── core/                  # Main protocol logic
│   ├── node.py            # Node representation & peer state
│   ├── routing_table.py   # Kademlia-style bucket routing table + reputation scores
│   ├── message.py         # Message formats (RequestClaimValidation, EvidenceChunk, etc.)
│   ├── protocol.py        # Main Protocol class (handles incoming/outgoing streams)
│   └── distance.py        # XOR distance + reputation-weighted distance metric
├── transport/             # Network layer abstractions
│   ├── quic_transport.py  # Production transport (QUIC + noise)
│   └── mock_transport.py  # In-process transport for tests & simulations
├── cli/                   # Developer & debug CLI tools
│   └── debug_node.py      # Run a single node with --port, --bootstrap etc.
├── tests/                 # 100% coverage goal
│   ├── unit/
│   ├── integration/
│   └── simulation/        # Large-scale network simulations (10k+ nodes)
├── scripts/
│   └── bootstrap_nodes.json   # Initial public bootstrap nodes (mainnet)
└── README.md              # You are here
```

## Dependencies

```bash
pip install aioquic==0.9.25
pip install noiseprotocol
pip install cryptography
pip install msgpack
pip install pytest pytest-asyncio
```

(Exact versions are pinned in `requirements.txt` at repo root)

## Quick Start – Run a Local Node

```bash
# Terminal 1 – bootstrap node
python -m src.routing_protocol.cli.debug_node --port 9000 --node-id bootstrap1

# Terminal 2 – regular node, joins via bootstrap
python -m src.routing_protocol.cli.debug_node \
    --port 9001 \
    --bootstrap /ip4/127.0.0.1/udp/9000/quic \
    --node-id alice-research-lab
```

You should see successful handshakes and routing table population.

## Core Concepts You Must Understand

1. **NodeID**  
   256-bit identifier derived as `BLAKE3(node_public_key)` → fixed proximity space.

2. **ClaimHash**  
   Canonical hash of a scientific claim (see `claim_extractor` spec). Used as the key nodes route toward.

3. **Reputation-Weighted XOR Distance**  
   ```python
   effective_distance = xor_distance * (1 + penalty_factor) / (1 + reputation_bonus)
   ```
   Malicious or unreliable nodes appear “farther away”.

4. **Message Types (message.py)**
   - `FIND_VALIDATORS` → returns k closest reputable nodes for a ClaimHash
   - `STORE_EVIDENCE` → gossip evidence packages
   - `VALIDATION_RESULT` → signed result propagation
   - `REPUTATION_UPDATE` → cryptographically signed feedback

## Extending the Protocol

### Adding a new message type
1. Add to `MessageType` enum in `message.py`
2. Add serialization/deserialization in `Message.pack()` / `Message.unpack()`
3. Handle it in `Protocol._handle_message()` (core/protocol.py)

### Changing routing table size (k, α, etc.)
All constants are in `core/routing_table.py`:
```python
K = 20          # bucket size
ALPHA = 3       # parallel requests
REPUTATION_HALFLIFE = 30 days
```

### Switching transport
Implement the abstract class in `transport/base.py` and change the import in `protocol.py`.

## Testing Strategy

```bash
# Unit tests
pytest tests/unit/

# Integration (real QUIC)
pytest tests/integration/

# Large-scale simulation (10–50k nodes, no real network)
python tests/simulation/run_simulation.py --nodes 10000 --malicious 5%
```

We aim for **100% line coverage** on `core/` – CI will fail otherwise.

## Security & Cryptography Notes

- All nodes have an Ed25519 keypair (stored in `~/.osvp/node_key` by default)
- Every message is signed
- Reputation updates are signed by the validator and include a proof-of-work nonce (anti-spam)
- Evidence chunks are content-addressed with BLAKE3

Never ship raw private keys. Use `node_key.encrypt(passphrase)` helper.

## Mainnet Bootstrap Nodes (as of Dec 2025)

```json
[
  "/ip4/104.131.155.32/udp/9000/quic/p2p/12D3KooWRTx...",
  "/ip4/178.62.229.184/udp/9000/quic/p2p/12D3KooW9fY...",
  "/dns4/bootstrap.osvp.xyz/udp/9000/quic/p2p/12D3KooWR..."
]
```

Updated list: https://bootstrap.osvp.network

## Contributing

1. Fork → create feature branch
2. Write tests first (TDD strongly encouraged)
3. Run full test suite + simulation
4. Open PR with clear description of the scientific or performance impact

Large changes (e.g. new distance metric, switch to Scuttlebutt-style replication) require an OSVP Improvement Proposal (OIP) first.

## Who to Contact

- Current maintainer: @GoodRoyal
- Routing protocol channel: `#routing-wg` on OSVP Discord
- Security issues → security@osvp.network (encrypted only)

---

**Welcome aboard!**  
The routing layer is the backbone that makes decentralized scientific validation possible at global scale. Your improvements here will directly accelerate trustworthy science.

Let’s route truth efficiently.

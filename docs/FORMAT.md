# Veridian wire formats

## Binary batch (VCB1)

Little-endian header:

| Offset | Size | Field |
|--------|------|-------|
| 0 | 4 | Magic `0x56434231` |
| 4 | 4 | Flags |
| 8 | 2 | Record count |
| 10 | 2 | Header CRC |
| 16 | n | Records |

Each record: `type(1) + length(2) + payload(n)`.

Set `DEFERRED_DIGEST` flag to stage native payload slots for pipeline digest.

## Envelope (VCE1)

Magic `0x56434531`, channel id, nested payload length, nested bytes.

## Session (VCS1)

Magic `0x56435331`, leg count, repeated leg reference bytes.

## FIX 4.4

Standard tag=value with SOH (`0x01`) delimiter. Collateral extensions use tags 909–930.

## SWIFT MT940

Block tags `:25:`, `:60F:`, `:61:`, `:62F:` with `$` block separator.

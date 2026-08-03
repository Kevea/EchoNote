# Getting notes into your Obsidian vault

Plainvoice writes Markdown into a folder on your phone. It does not sync
anything itself — it has no network access at all. Syncing is Syncthing's job,
and Obsidian just reads the folder.

Three pieces, each doing one thing:

```
Plainvoice  ──writes──▶  phone folder  ◀──syncs──▶  computer / vault
                              ▲
                              └── Obsidian (mobile) opens it directly
```

## 1. Plainvoice: pick the folder to export into

1. Open **Settings**
2. Under **Export format**, choose **Markdown (.md)** — that is what Obsidian
   reads. Notes are written with YAML frontmatter containing the tags and the
   creation date.
3. Under **Export folder**, tap **Choose folder** and pick (or create) a folder
   on your phone. Something like `Documents/Vault/Inbox` works well.
4. Under **Sync folder**, you decide per Plainvoice folder what happens:
   - **Sync automatically** on — notes are written out as soon as you tag them
   - **Copy** keeps the note in Plainvoice as well; **Move** removes it from the
     app once it has been written out

A note is exported **when you tag it**. That is the deliberate moment where you
have decided the note is worth keeping — not every half-finished recording ends
up in your vault.

## 2. Syncthing: keep that folder in sync

Install [Syncthing](https://syncthing.net/) on the phone and on the machine
holding your vault, then:

1. Pair the two devices with each other
2. On the phone, share the folder you picked in step 1
3. On the computer, accept it and point it at the matching folder inside your
   vault
4. Leave the folder type on **Send & Receive** on both sides — edits made on the
   computer then travel back to the phone

Syncthing talks directly between your devices. If you want it to work away from
home without opening ports, put both devices on a private network such as
[Tailscale](https://tailscale.com/) and let Syncthing use those addresses.

### Two things worth setting

- **Ignore patterns:** add `.trash` and `.obsidian/workspace*` so Obsidian's
  per-device UI state does not bounce between machines
- **File versioning:** Syncthing's *Simple File Versioning* on the computer side
  gives you a safety net if something is deleted by accident

## 3. Obsidian on the phone

Point Obsidian at the **same folder** Plainvoice writes into — either open that
folder as a vault, or place it inside an existing vault.

Because everything is plain files in a plain folder, there is no import step and
nothing to convert. A note recorded on the phone is a `.md` file the moment it
is tagged, and Obsidian sees it as soon as it appears.

## Troubleshooting

**Nothing arrives in the folder.** Notes are only exported when you tag them.
Add a tag to a note and check again.

**Plainvoice says it cannot write.** Android can revoke folder access, for
instance after the folder is moved or renamed. Open Settings, tap **Change
folder** and pick it again.

**Syncthing on Android goes quiet after a while.** Android's battery
optimisation suspends it. Exempt Syncthing from battery optimisation in the
system settings.

**Conflicting versions appear.** Syncthing keeps both sides and marks the loser
with `.sync-conflict-` in the name. Nothing is lost; open both and merge by hand.

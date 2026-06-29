package coder

import (
	"fmt"
	"os"

	specs "github.com/opencontainers/runtime-spec/specs-go"
)

// MountType represents whether a bind mount source is a file or directory.
type MountType int

const (
	MountTypeDirectory MountType = iota
	MountTypeFile
)

// DetectMountType inspects the source path and returns whether it is a file
// or directory. This is used to select the correct mount options: file sources
// must not use "rbind" (MS_BIND|MS_REC), which is only valid for directories.
func DetectMountType(src string) (MountType, error) {
	info, err := os.Stat(src)
	if err != nil {
		return MountTypeDirectory, fmt.Errorf("stat mount source %q: %w", src, err)
	}
	if info.IsDir() {
		return MountTypeDirectory, nil
	}
	return MountTypeFile, nil
}

// NewBindMount creates an OCI mount spec for a bind mount from src to dst.
// It detects whether src is a file or directory and sets mount options
// accordingly, preventing the "not a directory" error that occurs when
// MS_REC is applied to a file source.
func NewBindMount(src, dst string, readonly bool) (specs.Mount, error) {
	mountType, err := DetectMountType(src)
	if err != nil {
		return specs.Mount{}, err
	}

	options := []string{"bind"}
	if mountType == MountTypeDirectory {
		// rbind (MS_BIND|MS_REC) is only valid for directories.
		options = append(options, "rbind")
	}
	if readonly {
		options = append(options, "ro")
	}

	return specs.Mount{
		Source:      src,
		Destination: dst,
		Type:        "bind",
		Options:     options,
	}, nil
}

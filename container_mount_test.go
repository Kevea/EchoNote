package coder_test

import (
	"os"
	"path/filepath"
	"testing"

	coder "github.com/coder/coder"
)

func TestDetectMountType_File(t *testing.T) {
	f, err := os.CreateTemp(t.TempDir(), "*.sqlite")
	if err != nil {
		t.Fatal(err)
	}
	f.Close()

	mt, err := coder.DetectMountType(f.Name())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if mt != coder.MountTypeFile {
		t.Errorf("expected MountTypeFile, got %v", mt)
	}
}

func TestDetectMountType_Directory(t *testing.T) {
	dir := t.TempDir()

	mt, err := coder.DetectMountType(dir)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if mt != coder.MountTypeDirectory {
		t.Errorf("expected MountTypeDirectory, got %v", mt)
	}
}

func TestNewBindMount_FileDoesNotIncludeRbind(t *testing.T) {
	f, err := os.CreateTemp(t.TempDir(), "*.sqlite")
	if err != nil {
		t.Fatal(err)
	}
	f.Close()

	mount, err := coder.NewBindMount(f.Name(), "/openclaw-state/openclaw.sqlite", false)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	for _, opt := range mount.Options {
		if opt == "rbind" {
			t.Errorf("file mount must not include rbind option (triggers MS_REC which fails for files)")
		}
	}

	found := false
	for _, opt := range mount.Options {
		if opt == "bind" {
			found = true
		}
	}
	if !found {
		t.Error("mount options must include 'bind'")
	}
}

func TestNewBindMount_DirectoryIncludesRbind(t *testing.T) {
	dir := t.TempDir()

	mount, err := coder.NewBindMount(dir, "/some/dst", false)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	found := false
	for _, opt := range mount.Options {
		if opt == "rbind" {
			found = true
		}
	}
	if !found {
		t.Error("directory mount options must include 'rbind'")
	}
}

func TestNewBindMount_ReadonlyAddsRoOption(t *testing.T) {
	dir := t.TempDir()

	mount, err := coder.NewBindMount(dir, "/dst", true)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	found := false
	for _, opt := range mount.Options {
		if opt == "ro" {
			found = true
		}
	}
	if !found {
		t.Error("readonly mount must include 'ro' option")
	}
}

func TestNewBindMount_MissingSource(t *testing.T) {
	_, err := coder.NewBindMount(filepath.Join(t.TempDir(), "nonexistent.sqlite"), "/dst", false)
	if err == nil {
		t.Error("expected error for missing source path")
	}
}
